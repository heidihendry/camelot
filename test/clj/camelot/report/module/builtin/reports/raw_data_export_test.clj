(ns camelot.report.module.builtin.reports.raw-data-export-test
  (:require [camelot.report.core :as sut]
            [midje.sweet :refer :all]
            [clj-time.core :as t]
            [camelot.test-util.state :as state]))

(defn- gen-state-helper
  [config]
  (state/gen-state (merge {:language :en} config)))

(defn report
  [state id data]
  (sut/report :raw-data-export state {:survey-id id} data))

(defn csv-report
  [state id data]
  (sut/csv-report :raw-data-export state {:survey-id id} data))

(def headings ["Media Filename"
               "Media Capture Timestamp"
               "Site Name"
               "Site Sublocation"
               "Trap Station Name"
               "Trap Station Longitude"
               "Trap Station Latitude"
               "Genus"
               "Species"
               "Sighting Quantity"])

(def default-record
  {:site-id 1
   :site-name "ASite"
   :site-area 1
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
   :taxonomy-id nil
   :taxonomy-species "Yellow Spotted"
   :taxonomy-genus "Cat"
   :sighting-quantity 1
   :sighting-id 1})

(defn ->record
  [r]
  (merge default-record r))

(facts "Raw Data Export"
  (fact "Export without records is empty"
    (let [records '()
          state (gen-state-helper {})
          result (report state 1 records)]
      result => '()))

  (fact "Should export as expected for a single record"
    (let [records (list (->record {}))
          state (gen-state-helper {})]
      (report state 1 records) =>
      [["file-id-1" "2015-01-07 05:00:00" "ASite" nil
        "Trap1" -25 130 "Cat" "Yellow Spotted" 1]]))

  (fact "Should display separate rows for 2 media."
    (let [records (list
                   (->record {})
                   (->record {:media-id 2
                              :media-filename "file-id-2"}))
          state (gen-state-helper {})]
      (report state 1 records) =>
      [["file-id-1" "2015-01-07 05:00:00" "ASite" nil
        "Trap1" -25 130 "Cat" "Yellow Spotted" 1]
       ["file-id-2" "2015-01-07 05:00:00" "ASite" nil
        "Trap1" -25 130 "Cat" "Yellow Spotted" 1]]))

  (fact "Should display record even without having a sighting."
    (let [records (list
                   (->record {})
                   (->record {:sighting-id nil
                              :sighting-quantity nil
                              :sighting-species nil
                              :taxonomy-genus nil
                              :taxonomy-species nil
                              :media-id 2
                              :media-filename "file-id-2"}))
          state (gen-state-helper {})]
      (report state 1 records) =>
      [["file-id-2" "2015-01-07 05:00:00" "ASite" nil
        "Trap1" -25 130 nil nil nil]
       ["file-id-1" "2015-01-07 05:00:00" "ASite" nil
        "Trap1" -25 130 "Cat" "Yellow Spotted" 1]]))

  (fact "Should display 2 records for two different sightings on an image"
    (let [records (list
                   (->record {})
                   (->record {:media-id 2
                              :media-filename "file-id-2"})
                   (->record {:sighting-quantity 2
                              :sighting-id 2
                              :taxonomy-genus "Bird"}))
          state (gen-state-helper {})]
      (report state 1 records) =>
      [["file-id-1" "2015-01-07 05:00:00" "ASite" nil
        "Trap1" -25 130 "Bird" "Yellow Spotted" 2]
       ["file-id-1" "2015-01-07 05:00:00" "ASite" nil
        "Trap1" -25 130 "Cat" "Yellow Spotted" 1]
       ["file-id-2" "2015-01-07 05:00:00" "ASite" nil
        "Trap1" -25 130 "Cat" "Yellow Spotted" 1]])))
