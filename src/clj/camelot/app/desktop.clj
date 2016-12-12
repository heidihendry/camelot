(ns camelot.app.desktop
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
      (if (Desktop/isDesktopSupported)
        (.browse (Desktop/getDesktop) uri))
      (catch java.lang.UnsupportedOperationException e
        (sh "bash" "-c" (str "xdg-open " addr " &> /dev/null &"))))))
