(ns ctdp.config
  (:require [ctdp.translations.core :refer :all]
            [taoensso.tower :as tower]))

(def config
  "Application configuration"
  {:erroneous-infrared-threshold 0.2
   :infrared-iso-value-threshold 999
   :language :en
   :night-end-hour 5
   :night-start-hour 21
   :problems {:datetime :warn}
   :rename {:format "%s-%s"
            :fields [[:datetime] [:camera :model]]
            :date-format "YYYY-MM-dd HH.mm.ss"}})

(defn gen-translator
  "Create a translator for the user's preferred language."
  [config]
  (let [tlookup (partial (tower/make-t tconfig) (:language config))]
    (fn [t & vars]
      (apply format (tlookup t) vars))))

(defn gen-state
  "Return the global application state."
  [conf]
  {:config conf
   :translate (gen-translator conf)})
