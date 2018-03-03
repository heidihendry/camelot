(ns camelot.util.misc
  "Miscellaneous utilities."
  (:require
   [camelot.util.url :as url]
   [cljs-time.core :as t]))

(defn nights-elapsed
  "Calculate the number of `nights' between two dates."
  [start end]
  (t/in-days (t/interval (t/at-midnight start) (t/at-midnight end))))

(defn get-host
  []
  (some-> (-> js/window (aget "location") (aget "search"))
          (subs 1)
          (.split "&")
          first
          (.split "=")
          second))

(defn with-baseurl
  "Return the given path along with the correct base URL."
  [path]
  (let [u (url/window-href)
        protocol (:protocol u)
        port (:port u)]
    (if (or (clojure.string/starts-with? protocol "http")
            (clojure.string/starts-with? protocol "https"))
      (str protocol "://" (:host u)
           (when port (str ":" port))
           path)
      (str "http://localhost:3449" path))))
