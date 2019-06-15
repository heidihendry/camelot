(ns camelot.nav-util
  (:require [clojure.string :as str]))

(defn nav-up-url
  [token levels]
  {:pre [(and (string? token) (number? levels))]}
  (let [url (reduce #(str/replace % #"(.*)/.+?$" "$1") token (range levels))]
    (if (or (= url "/#") (= url ""))
      "/#/organisation"
      url)))
