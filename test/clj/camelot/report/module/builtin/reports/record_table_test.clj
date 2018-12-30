(ns camelot.report.module.builtin.reports.record-table-test
  (:require
   [camelot.report.core :as report]
   [clojure.test :refer :all :exclude [report]]
   [camelot.testutil.state :as state]
   [clj-time.core :as t]
   [clojure.string :as str]))

(defn- gen-state-helper
  [config]
  (state/gen-state (merge {:language :en
                           :paths {:media "/path"}} config)))

(defn report
  [state id data]
  (with-redefs [camelot.model.sighting-field/get-all (constantly [])
                camelot.model.survey/survey-settings (constantly {})]
    (report/report :record-table state {:survey-id id} data)))

(defn csv-report
  [state id data]
  (with-redefs [camelot.model.sighting-field/get-all (constantly [])
                camelot.model.survey/survey-settings (constantly {})]
    (report/csv-report :record-table state {:survey-id id} data)))

(def headings ["Station"
               "Camera"
               "CameraName"
               "Species"
               "TrapAndCamera"
               "DateTimeOriginal"
               "Date"
               "Time"
               "delta.time.secs"
               "delta.time.mins"
               "delta.time.hours"
               "delta.time.days"
               "Directory"
               "FileName"])

(def default-record
  {:site-id 1
   :site-name "ASite"
   :site-area 1
   :camera-name "CAM1"
   :trap-station-session-camera-id 1
   :trap-station-id 1
   :trap-station-session-start-date (t/date-time 2015 1 1 10 10 10)
   :trap-station-session-end-date (t/date-time 2015 1 10 5 0 0)
   :trap-station-session-id 1
   :trap-station-name "Trap1"
   :trap-station-latitude -25
   :trap-station-longitude 130
   :survey-id 1
   :survey-site-id 1
   :media-id 1
   :media-capture-timestamp (t/date-time 2015 1 7 5 0 0)
   :media-filename "file-id-1"
   :media-format "jpg"
   :taxonomy-id 39
   :taxonomy-species "Yellow Spotted"
   :taxonomy-genus "Cat"
   :sighting-quantity 1
   :sighting-id 1})

(defn ->record
  [r]
  (merge default-record r))

(defn ->alt-record
  [r]
  (merge default-record {:taxonomy-id 40
                         :camera-name "CAM2"
                         :trap-station-id 1
                         :trap-station-session-camera-id 2
                         :trap-station-name "Trap2"
                         :taxonomy-species "Smiley"
                         :media-filename "file-id-2"
                         :taxonomy-genus "Wolf"} r))

(deftest test-record-table
  (testing "Record Table data"
    (testing "Export without records is empty"
      (let [records '()
            state (gen-state-helper {})
            result (report state 1 records)]
        (is (= result '()))))

    (testing "Should export basic data for a single record"
      (let [records (list (->record {}))
            state (gen-state-helper {})]
        (is (= (report state 1 records)
               [["Trap1" 1 "CAM1" "Cat Yellow Spotted" "Trap1_1" "2015-01-07 05:00:00" "2015-01-07" "05:00:00"
                 "0" "0" "0.0" "0.0" "/path/fi" "file-id-1.jpg"]]))))

    (testing "Should include time delta columns for independent sightings"
      (let [records (list (->record {:media-capture-timestamp (t/date-time 2015 01 07 5 0 0)})
                          (->record {:media-capture-timestamp (t/date-time 2015 01 07 5 30 0)}))
            state (gen-state-helper {})]
        (is (= (report state 1 records)
               [["Trap1" 1 "CAM1" "Cat Yellow Spotted" "Trap1_1" "2015-01-07 05:00:00" "2015-01-07" "05:00:00"
                 "0" "0" "0.0" "0.0" "/path/fi" "file-id-1.jpg"]
                ["Trap1" 1 "CAM1" "Cat Yellow Spotted" "Trap1_1" "2015-01-07 05:30:00" "2015-01-07" "05:30:00"
                 "1800" "30" "0.5" "0.0" "/path/fi" "file-id-1.jpg"]]))))

    (testing "Should omit records which are dependent"
      (let [records (list (->record {:media-capture-timestamp (t/date-time 2015 01 07 5 0 0)})
                          (->record {:media-capture-timestamp (t/date-time 2015 01 07 5 10 0)})
                          (->record {:media-capture-timestamp (t/date-time 2015 01 07 5 30 0)}))
            state (gen-state-helper {})]
        (is (= (report state 1 records)
               [["Trap1" 1 "CAM1" "Cat Yellow Spotted" "Trap1_1" "2015-01-07 05:00:00" "2015-01-07" "05:00:00"
                 "0" "0" "0.0" "0.0" "/path/fi" "file-id-1.jpg"]
                ["Trap1" 1 "CAM1" "Cat Yellow Spotted" "Trap1_1" "2015-01-07 05:30:00" "2015-01-07" "05:30:00"
                 "1800" "30" "0.5" "0.0" "/path/fi" "file-id-1.jpg"]]))))

    (testing "Should allow for a mix of trap stations and species"
      (let [records (list (->alt-record {:media-capture-timestamp (t/date-time 2015 01 07 5 0 0)})
                          (->record {:media-capture-timestamp (t/date-time 2015 01 07 5 15 0)})
                          (->alt-record {:media-capture-timestamp (t/date-time 2015 01 07 5 30 0)})
                          (->record {:media-capture-timestamp (t/date-time 2015 01 07 5 30 0)}))
            state (gen-state-helper {})]
        (is (= (report state 1 records)
               [["Trap2" 2 "CAM2" "Wolf Smiley" "Trap2_2" "2015-01-07 05:00:00" "2015-01-07" "05:00:00"
                 "0" "0" "0.0" "0.0" "/path/fi" "file-id-2.jpg"]
                ["Trap1" 1 "CAM1" "Cat Yellow Spotted" "Trap1_1" "2015-01-07 05:15:00" "2015-01-07" "05:15:00"
                 "0" "0" "0.0" "0.0" "/path/fi" "file-id-1.jpg"]
                ["Trap2" 2 "CAM2" "Wolf Smiley" "Trap2_2" "2015-01-07 05:30:00" "2015-01-07" "05:30:00"
                 "1800" "30" "0.5" "0.0" "/path/fi" "file-id-2.jpg"]]))))

    (testing "Should cope with records being out of order"
      (let [records (list (->record {:media-capture-timestamp (t/date-time 2015 01 07 5 30 0)})
                          (->record {:media-capture-timestamp (t/date-time 2015 01 07 5 15 0)})
                          (->alt-record {:media-capture-timestamp (t/date-time 2015 01 07 5 30 0)})
                          (->alt-record {:media-capture-timestamp (t/date-time 2015 01 07 5 0 0)}))
            state (gen-state-helper {})]
        (is (= (report state 1 records)
               [["Trap2" 2 "CAM2" "Wolf Smiley" "Trap2_2" "2015-01-07 05:00:00" "2015-01-07" "05:00:00"
                 "0" "0" "0.0" "0.0" "/path/fi" "file-id-2.jpg"]
                ["Trap1" 1 "CAM1" "Cat Yellow Spotted" "Trap1_1" "2015-01-07 05:15:00" "2015-01-07" "05:15:00"
                 "0" "0" "0.0" "0.0" "/path/fi" "file-id-1.jpg"]
                ["Trap2" 2 "CAM2" "Wolf Smiley" "Trap2_2" "2015-01-07 05:30:00" "2015-01-07" "05:30:00"
                 "1800" "30" "0.5" "0.0" "/path/fi" "file-id-2.jpg"]])))))


  (testing "Record Table CSV"
    (testing "Export without records has header columns"
      (let [records '()
            state (gen-state-helper {})
            result (csv-report state 1 records)]
        (is (= result (str (str/join "," headings) "\n")))))

    (testing "Should export basic data for a single record"
      (let [records (list (->record {}))
            state (gen-state-helper {})]
        (is (= (csv-report state 1 records)
               (str (str/join "," headings) "\n"
                    "Trap1,1,CAM1,Cat Yellow Spotted,Trap1_1,2015-01-07 05:00:00,2015-01-07,05:00:00,0,0,0.0,0.0,/path/fi,file-id-1.jpg\n")))))))
