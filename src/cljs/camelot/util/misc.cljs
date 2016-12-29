(ns camelot.util.misc
  "Miscellaneous utilities."
  (:require
   [cljs-time.core :as t]))

(defn nights-elapsed
  "Calculate the number of `nights' between two dates."
  [start end]
  (t/in-days (t/interval (t/at-midnight start) (t/at-midnight end))))

(defn with-baseurl
  "Return the given path along with the correct base URL."
  [path]
  (let [protocol (-> js/window (aget "location") (aget "protocol"))
        port (-> js/window (aget "location") (aget "port"))]
    (if (or (clojure.string/starts-with? protocol "http")
            (clojure.string/starts-with? protocol "https"))
      (str
       (-> js/window (aget "location") (aget "protocol"))
       "//"
       (-> js/window (aget "location") (aget "hostname"))
       (when-not (zero? (count port))
         (str ":" port))
       path)
      (str "http://localhost:3449" path))))
