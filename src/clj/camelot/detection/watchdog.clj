(ns camelot.detection.watchdog
  (:require
   [camelot.detection.client :as client]
   [clojure.tools.logging :as log]
   [clojure.core.async :as async]))

(def ^:private check-health-timeout (* 30 1000))

(defn- desired-state
  [system-state]
  (if (client/healthy? system-state)
    :running
    :paused))

(defn- overridable-paused-status?
  [sys]
  (and (= (:status sys) :paused)
       (not= (:set-by sys) :user)))

(defn- running-status?
  [sys]
  (= (:status sys) :running))

(defn- overridable-status?
  [sys]
  (some #(% sys)
        [running-status? overridable-paused-status?]))

(def ^:private status-command-map
  {:running :resume
   :paused :pause})

(defn run
  "Pause and resume the system according to connectivity."
  [system-state detector-state-ref cmd-mult cmd-pub-ch event-ch]
  (let [cmd-ch (async/tap cmd-mult (async/chan))]
    (async/go-loop []
      (let [timeout-ch (async/timeout check-health-timeout)
            [v port] (async/alts! [cmd-ch timeout-ch])]
        (condp = port
          cmd-ch
          (if (= (:cmd v) :stop)
            (log/info "Detector watchdog stopped")
            (recur))

          timeout-ch
          (do
            (let [system @(:system detector-state-ref)]
              (when (overridable-status? system)
                (let [desired (desired-state system-state)
                      actual (:status system)]
                  (when (not= desired actual)
                    (let [cmd (status-command-map desired)]
                      (async/go
                        (async/>! cmd-pub-ch {:cmd cmd})
                        (async/>! event-ch {:action (keyword (str "watchdog-initiated-" (name cmd)))
                                            :subject :global})))))))
            (recur)))))))
