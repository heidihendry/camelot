(ns ctdp.translations.core
  (:require [ctdp.translations.en :refer :all])
  (:require [ctdp.translations.vn :refer :all]))

(def tconfig
  "Configuration for translations."
  {:dictionary
   {:en t-en
    :vn t-vn}
   :dev-mode? true
   :fallback-locale :en})
