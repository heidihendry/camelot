(ns camelot.config
  (:require [camelot.translations.core :refer :all]
            [clj-time.core :as t]
            [taoensso.tower :as tower]))

(def config
  "Application configuration"
  {:erroneous-infrared-threshold 0.2
   :infrared-iso-value-threshold 999
   :language :en
   :night-end-hour 5
   :night-start-hour 21
   :project-start (t/date-time 2014 03 14)
   :project-end (t/date-time 2015 01 24)
   :problems {:datetime :warn}
   :sighting-independence-minutes-threshold 20
   :surveyed-species ["Atherurus macrourus"
                      "Cervus unicolor"
                      "Chrotogale owstoni"
                      "Homo sapiens"
                      "HUMAN-CAMERACHECK"
                      "Lophura diardi"
                      "Lophura nycthemera"
                      "Macaca arctoides"
                      "Macaca assamensis"
                      "Macaca mulatta"
                      "Martes flavigula"
                      "Muntiacus muntjac"
                      "Nomascus concolor"
                      "Nycticebus bengalensis"
                      "Paguma larvata"
                      "Paradoxurus hermaphroditus"
                      "Pardofelis marmorata"
                      "Prionailurus bengalensis"
                      "Prionodon pardicolor"
                      "Sus scrofa"
                      "Tragopan temmnickii"
                      "Ursus thibetanus"]
   :required-fields [[:headline] [:artist] [:phase] [:copyright]
                     [:location :gps-longitude] [:location :gps-longitude-ref]
                     [:location :gps-latitude] [:location :gps-latitude-ref]
                     [:datetime] [:filename]]
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
