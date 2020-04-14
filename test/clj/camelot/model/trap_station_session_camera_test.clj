(ns camelot.model.trap-station-session-camera-test
  "Tests around the trap station session camera model."
  (:require
   [clojure.test.check]
   [clojure.spec.alpha :as s]
   [camelot.spec.db :as dbspec]
   [camelot.testutil.db :as tdb]
   [camelot.testutil.spec :as tspec]
   [clojure.test :refer [use-fixtures]]
   [camelot.model.trap-station-session-camera :as sut]))

(s/def ::camera-usage-result
  (s/keys :req-un [::dbspec/trap_station_id
                   ::dbspec/trap_station_name
                   ::dbspec/camera_id
                   ::dbspec/survey_id
                   ::dbspec/trap_station_session_id
                   ::dbspec/trap_station_session_start_date
                   ::dbspec/trap_station_session_end_date]))

(defn -get-camera-usage [& args])
(s/fdef -get-camera-usage
  :args (s/cat :data (s/keys :req-un [::dbspec/camera_id])
               :connection ::tdb/db-connection)
  :ret (s/coll-of ::camera-usage-result :kind vector? :count 3))

(defn use-instrumentation [f]
  (tdb/with-queries {:trap-station-session-cameras
                     {:get-camera-usage -get-camera-usage}}
    (tspec/with-stubs [`-get-camera-usage]
      (f))))

(use-fixtures :once use-instrumentation)

(let [opts {:clojure.spec.test.check/opts {:num-tests 10}}]
  (tspec/defspec-test testspec-get-camera-usage `sut/get-camera-usage opts))
