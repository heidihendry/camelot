(ns ctdp.config
  (:require [ctdp.translations.core :refer :all]
            [taoensso.tower :as tower]))

(def config
  {:erroneous-infrared-threshold 0.2
   :infrared-iso-value-threshold 999
   :language :en
   :night-end-hour 5
   :night-start-hour 21
   :problems {:datetime :warn}
   :rename {:format "%s-%s"
            :fields [[:camera :make] [:camera :model]]}})

(def state
  {:config config
   :translations (tower/make-t tconfig)})
