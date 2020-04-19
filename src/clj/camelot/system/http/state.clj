(ns camelot.system.http.state)

(defn wrap-state
  [handler]
  (fn [request]
    (let [state (merge (:system request)
                       (select-keys request [:session]))]
      (handler (assoc request :state state)))))
