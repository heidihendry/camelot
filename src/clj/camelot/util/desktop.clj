(ns camelot.util.desktop
  (:require
   [clojure.java.shell :refer [sh]])
  (:import
   (java.net URI)
   (java.awt Desktop)))

(defn start-browser
  [port]
  (let [addr (str "http://localhost:" port "/")
        uri (new URI addr)]
    (try
      (when (Desktop/isDesktopSupported)
        (.browse (Desktop/getDesktop) uri))
      (catch java.lang.UnsupportedOperationException _
        (sh "bash" "-c" (str "xdg-open " addr " &> /dev/null &"))))))
