(ns camelot.system.cli
  (:require
   [environ.core :refer [env]]))

(def option-defaults
  {:port (or (some-> (env :camelot-port) (Integer/parseInt)) 5341)
   :browser false})

(def cli-opts
  [["-p" "--port PORT" "Port number"
    :id :port
    :default (:port option-defaults)
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 0x10000) "Must be a valid port number"]]
   [nil "--browser" "Start browser"
    :id :browser
    :default (:browser option-defaults)]])
