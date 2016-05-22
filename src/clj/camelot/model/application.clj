(ns camelot.model.application
  (:require [camelot.translation.core :as tr]
            [camelot.util
             [config :as settings]
             [feature :as feature]]))

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
  {:menu-items [{:url "/#/dashboard" :label (tr/translate (:config state) :application/dashboard)}
                {:url "/#/surveys" :label (tr/translate (:config state) :application/surveys)}
                {:url "/#/sites" :label (tr/translate (:config state) :application/sites)}
                {:url "/#/cameras" :label (tr/translate (:config state) :application/cameras)}
                {:url "/#/analysis" :label (tr/translate (:config state) :application/analysis)}
                {:function "settings"}]})
