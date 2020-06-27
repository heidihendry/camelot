(ns camelot.system.detector
  "Wildlife detector component."
  (:require
   [camelot.protocols :as protocols]
   [camelot.detection.learn :as learn]
   [camelot.services.analytics :as analytics]
   [camelot.util.state :as state]
   [duratom.core :as duratom]
   [com.stuartsierra.component :as component]
   [clojure.core.async :as async]
   [camelot.detection.core :as detection]
   [clojure.java.io :as io]
   [clojure.tools.logging :as log]
   [taoensso.nippy :as nippy])
  (:import
   (java.io File)))

(defn detector-filename
  "Name of the detector state file."
  [state]
  (str (-> state :config :detector :username)
       "-"
       (name (-> state :session :dataset-id))
       "-detector.edn"))

(def state-serialisation-backoff
  "Delay in ms between attempts to write out the current detector state.
  Idempotence is order-of-the-day; we afford a few seconds of loss."
  (* 15 1000))

(defn- detector-path
  [state]
  (let [file (io/file (-> state :config :paths :database)
                      (detector-filename state))]
    (.getCanonicalPath ^File file)))

(defn- event-to-analytics
  [v]
  (merge
   {:category "detector"
    :action (name (:action v))
    :label (if-let [s (:subject v)] (name s))
    :label-value (let [lv (:subject-id v)] (when (int? lv) lv))
    :ni true}
   (:meta v)))

(defn- command-to-analytics
  [v]
  (merge
   {:category "detector"
    :action (str (name (:cmd v)) "-command")
    :ni (= (:set-by v) :user)}
   (:meta v)))

(defn- command-to-status
  [cmd]
  (condp = cmd
    :stop :stopped
    :pause :paused
    :resume :running))

(defn- event-to-system-status
  [evt]
  {:status (command-to-status (:cmd evt))
   :set-by (:set-by evt)})

(defn- run-state-file-manager
  [detector-state state-update-chan cmd-mult]
  (let [int-cmd-ch (async/tap cmd-mult (async/chan))]
    (async/go-loop []
      (let [timeout-ch (async/timeout state-serialisation-backoff)
            [v port] (async/alts! [int-cmd-ch timeout-ch])]
        (condp = port
          int-cmd-ch
          (condp = (:cmd v)
            :stop
            (do
              (if-let [[file data] (async/poll! state-update-chan)]
                (nippy/freeze-to-file file data))
              (swap! (:system detector-state) merge {:status :stopped :set-by nil}))

            (do
              (log/info "Command received:" v)
              (recur)))

          timeout-ch
          (let [_ (async/<! timeout-ch)]
            (try
              (if-let [[file data] (async/poll! state-update-chan)]
                (nippy/freeze-to-file file data))
              (catch Exception e
                (log/error "Exception while writing state file" e)))
            (recur)))))))

(defn- run-analytics-publisher
  [system-state detector-state event-chan cmd-chan cmd-mult]
  (let [kf (juxt (constantly :events) :subject :action)
        int-cmd-ch (async/tap cmd-mult (async/chan))]
    (async/go-loop []
      (let [[v port] (async/alts! [int-cmd-ch event-chan])]
        (condp = port
          int-cmd-ch
          (do
            (log/info "Received event to detector command handler" v)
            (condp = (:cmd v)
              :stop (swap! (:system detector-state) merge (event-to-system-status v))
              :pause (swap! (:system detector-state) merge (event-to-system-status v))
              :resume (swap! (:system detector-state) merge (event-to-system-status v))
              nil)
            (async/go (async/>! event-chan v))
            (recur))

          event-chan
          (do
            (cond
              (= (:subject v) :system-status)
              (swap! (:system detector-state) merge {:status (:action v)})

              (nil? (:cmd v))
              (when (:dataset-id v)
                (swap! (get-in detector-state [:datasets (:dataset-id v)])
                       update-in (kf v) (fnil inc 0))))

            (log/info "Publishing analytics for" (:action v))
            (if (:cmd v)
              (analytics/track system-state (command-to-analytics v))
              (analytics/track system-state (event-to-analytics v)))
            (recur)))))))

(defn- init-dataset-detector
  [this system-state learn-chan]
  (if (:cmd-chan this)
    this
    (do
      (analytics/track system-state {:category "detector"
                                     :action "startup"
                                     :ni true})
      (log/info "Starting detector...")
      (let [state-update-chan (async/chan (async/sliding-buffer 1))
            detector-state
            (reduce
             (fn [acc dataset-id]
               (let [state (state/with-dataset system-state dataset-id)]
                 (assoc-in acc [:datasets dataset-id]
                           (duratom/duratom :local-file
                                            :file-path (detector-path state)
                                            :init {}
                                            :rw {:read nippy/thaw-from-file
                                                 :write (fn [file data]
                                                          (async/go (async/>! state-update-chan [file data])))}))))
             {:system (atom {})} (state/get-dataset-ids system-state))
            cmd-chan (async/chan)
            cmd-mult (async/mult cmd-chan)
            event-chan (detection/run system-state detector-state cmd-chan cmd-mult learn-chan)]
        (run-analytics-publisher system-state detector-state event-chan cmd-chan cmd-mult)
        (run-state-file-manager detector-state state-update-chan cmd-mult)
        (-> this
            (assoc :state detector-state)
            (assoc :cmd-chan cmd-chan))))))

(defprotocol Commandable
  (command [this cmd]))

(defrecord Detector [config database]
  Commandable
  ;; It's important with pause/resume that it happens for all detectors at
  ;; once. If this were not the case the core.async's thread pool will rapidly
  ;; become blocked waiting on paused threads.
  (command [this cmd]
    (when-let [chan (:cmd-chan this)]
      (async/put! chan cmd)))

  protocols/Learnable
  (learn [this media-with-dataset-id]
    (async/>!! (:learn-chan this) media-with-dataset-id))

  component/Lifecycle
  (start [this]
    (let [system-state {:config config :database database}]
      (if (-> system-state :config :detector :enabled)
        ;; :learn needs to go into this early
        (let [learn-chan (learn/run system-state)]
          (assoc (init-dataset-detector this system-state learn-chan)
                 :learn-chan learn-chan))
        this)))

  (stop [this]
    (if-let [chan (:cmd-chan this)]
      (do
        (log/info "Detector stopping...")
        (async/put! chan {:cmd :stop})
        (-> this
            (assoc :learn-chan nil)
            (assoc :state nil)
            (assoc :cmd-chan nil)))
      this)))
