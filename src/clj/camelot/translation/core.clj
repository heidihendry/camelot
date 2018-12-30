(ns camelot.translation.core
  (:require
   [camelot.translation.en :refer :all]
   [camelot.translation.vn :refer :all]
   [taoensso.tower :as tower]
   [camelot.util.state :as state]))

(def tconfig
  "Configuration for translations."
  {:dictionary
   {:en t-en
    :vn t-vn}
   :dev-mode? true
   :fallback-locale :en})

(defn translate
  "Create a translator for the user's preferred language."
  [state tkey & vars]
  (let [tlookup (partial (tower/make-t tconfig) (state/lookup state :language))]
    (apply format (tlookup tkey) vars)))
