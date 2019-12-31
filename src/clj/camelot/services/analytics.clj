(ns camelot.services.analytics
  (:require
   [camelot.util.version :as version]
   [clj-http.client :as http]))

(def socket-timeout (* 1 1000))
(def connection-timeout (* 1 1000))

(def tracking-id "UA-77556072-2")

(defn track
  [state {:keys [category action label label-value dimension1 metric1 ni]}]
  (future (http/post (str "https://ssl.google-analytics.com/collect?v=1"
                          "&t=" "event"
                          "&tid=" tracking-id
                          "&cid=" (-> state :config :client-id)
                          "&ni=" (if ni 1 0)
                          "&an=" "camelot"
                          "&av=" (version/get-version)
                          "&ec=" category
                          "&ea=" action
                          "&el=" label
                          "&ev=" label-value
                          "&cd1=" dimension1
                          "&cm1=" metric1)
                     {:socket-timeout socket-timeout
                      :connection-timeout connection-timeout})))

(defn track-timing
  [state {:keys [category variable time label dimension1 metric1 ni]}]
  (future (http/post (str "https://ssl.google-analytics.com/collect?v=1"
                          "&t=" "timing"
                          "&tid=" tracking-id
                          "&cid=" (-> state :config :client-id)
                          "&ni=" (if ni 1 0)
                          "&an" "camelot"
                          "&av=" (version/get-version)
                          "&utc=" category
                          "&utv=" variable
                          "&utl=" label
                          "&utt=" time
                          "&cd1=" dimension1
                          "&cm1=" metric1)
                     {:socket-timeout socket-timeout
                      :connection-timeout connection-timeout})))
