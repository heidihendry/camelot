(ns camelot.translations.core
  (:require [camelot.translations.en :refer :all])
  (:require [camelot.translations.vn :refer :all]))

(def tconfig
  "Configuration for translations."
  {:dictionary
   {:en t-en
    :vn t-vn}
   :dev-mode? true
   :fallback-locale :en})
