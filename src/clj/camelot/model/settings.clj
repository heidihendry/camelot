(ns camelot.model.settings
  (:require [camelot.processing.settings :as settings]
            [camelot.util.feature :as feature]))

(def metadata-structure
  [[:location [:gps-longitude
               :gps-longitude-ref
               :gps-latitude
               :gps-latitude-ref
               :gps-altitude
               :gps-altitude-ref
               :sublocation
               :city
               :state-province
               :country
               :country-code
               :map-datum]]
   [:camera-settings [:aperture
                      :exposure
                      :flash
                      :focal-length
                      :fstop
                      :iso
                      :orientation
                      :resolution-x
                      :resolution-y]]
   [:camera [:make
             :model
             :software]]
   :datetime
   :headline
   :artist
   :phase
   :copyright
   :description
   :filename
   :filesize])

(defn nav-menu
  [state]
  {:menu-items [{:url "/#/dashboard" :label ((:translate state) :application/dashboard)}
                (when (feature/enabled? :survey)
                  {:url "/#/surveys" :label ((:translate state) :application/surveys)})
                (when (feature/enabled? :survey)
                  {:url "/#/sites" :label ((:translate state) :application/sites)})
                (when (feature/enabled? :camera)
                  {:url "/#/cameras" :label ((:translate state) :application/cameras)})
                {:url "/#/analysis" :label ((:translate state) :application/analysis)}
                {:function "settings"}]})
