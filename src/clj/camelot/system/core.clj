(ns camelot.system.core
  "System lifecycle management."
  (:require
   [camelot.system.systems :as systems]
   [camelot.system.state :as state]
   [camelot.util.maintenance :as maintenance]
   [com.stuartsierra.component :as component]
   [clojure.core.async :refer [>!! <! chan go-loop]]))

(def ^:private lifecycle-chan-buf-size 1)
(def ^:private lifecycle nil)

(defn- wait
  "Send messages to the channel, using buffer size & blocking to create
  synchronous behaviour."
  [chan]
  (>!! chan {:action :wait})
  (>!! chan {:action :wait}))

(defn- switch-mode
  "Switch Camelot's lifecycle to the given mode."
  ([chan new-mode]
   (>!! chan {:action new-mode})
   (wait chan))
  ([chan new-mode payload]
   (>!! chan {:action new-mode
              :payload payload})
   (wait chan)))

(defprotocol Lifecycle
  (user-mode [_]
    "Switch to user mode synchronously.
  The passed state object becomes invalid after calling this.")
  (maintenance-mode [_]
    "Switch to maintenance mode synchronously.
  The passed state object becomes invalid after calling this."))

(defrecord LifecycleImpl [chan]
  Lifecycle
  (user-mode [_]
    (switch-mode chan :pre-init)
    (switch-mode chan :user))

  (maintenance-mode [_]
    (switch-mode chan :maintenance)))

(defn- stop-running-system
  []
  (when @state/system
    (swap! state/system component/stop)))

(defn- build-lifecycle
  [config]
  (let [ch (chan lifecycle-chan-buf-size)]
    (go-loop []
      (let [{:keys [action payload]} (<! ch)]
        (condp = action
          :pre-init
          (do
            (stop-running-system)
            (reset! state/system (systems/pre-init config payload)))

          :user
          (do
            (stop-running-system)
            (reset! state/system (systems/camelot config)))

          :maintenance
          (do
            (stop-running-system)
            (reset! state/system (systems/maintenance config)))
          nil))
      (recur))
    (LifecycleImpl. ch)))

(defn user-mode!
  "Switch Camelot to user-mode."
  []
  (user-mode lifecycle))

(defn maintenance-mode!
  "Switch Camelot to maintenance-mode."
  []
  (maintenance-mode lifecycle))

(defn begin
  "Create the application lifecycle."
  ([]
   (begin {}))
  ([config]
   (when-not lifecycle
     (alter-var-root #'lifecycle
                     (fn [_] (build-lifecycle config))))))
