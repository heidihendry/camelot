(ns camelot.system.http.log
  "Ring handler for request logging."
  (:require
   [clojure.tools.logging :as log]))

(defn- log-response
  [{:keys [request-method uri remote-addr]}
   {:keys [status]}
   elapsed]
  (let [req-str (format "%s %s for %s" request-method uri remote-addr )
        resp-str (format "Status: %s (%d ms)" status elapsed)]
    (log/info req-str "|" resp-str)))

(defn- stacktrace
  "Converts a Throwable into a sequence of strings with the stacktrace."
  [^Throwable throwable]
  (clojure.string/join "\n" (doall (map str (.getStackTrace throwable)))))

(defn- log-exception
  "Logging for exceptinos"
  [req throwable elapsed]
  (log-response req {:status 500} elapsed)
  (log/error "Uncaught exception processing request:\n" (.getMessage throwable) "\n" (stacktrace throwable)))

(defn wrap-with-logger
  "Request logging handler."
  [handler]
  (fn [request]
    (let [start (System/currentTimeMillis)]
      (try
        (let [response (handler request)
              elapsed (- (System/currentTimeMillis) start)]
          (log-response request response elapsed)
          response)
        (catch Throwable e
          (let [elapsed (- (System/currentTimeMillis) start)]
            (log-exception request e elapsed))
          (throw e))))))
