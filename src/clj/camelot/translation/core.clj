(ns camelot.translation.core
  (:require [camelot.translation.en :refer :all])
  (:require [camelot.translation.vn :refer :all]))

(def tconfig
  "Configuration for translations."
  {:dictionary
   {:en t-en
    :vn t-vn}
   :dev-mode? true
   :fallback-locale :en})
