(ns camelot.detection.util
  (:require
   [clojure.core.async :as async]
   [clojure.tools.logging :as log]))

(defmacro pause
  "Handle pause commands.
  This is a macro to preserve the logger's namespace in the log output."
  ([cmd-ch propagate]
   `(do
      (log/info "Pausing")
      (loop []
        (let [v# (async/<! ~cmd-ch)
              {cmd# :cmd} v#]
          (log/info "Received command" cmd#)
          (~propagate v#)
          (if (= cmd# :resume)
            (log/info "Resuming")
            (recur))))
      (recur))))
