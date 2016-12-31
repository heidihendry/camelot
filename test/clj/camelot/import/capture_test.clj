(ns camelot.import.capture-test
  (:require [camelot.import.capture :as sut]
            [clojure.test :refer :all]
            [clj-time.core :as t]
            [camelot.model.trap-station-session :as trap-station-session]))

(def default-trap-station-session
  {:trap-station-session-id 1
   :trap-station-session-created (t/date-time 2017 1 1)
   :trap-station-session-updated (t/date-time 2017 1 1)
   :trap-station-id 2
   :trap-station-session-start-date (t/date-time 2016 11 1)
   :trap-station-session-end-date (t/date-time 2016 12 1)
   :trap-station-session-notes nil
   :trap-station-session-label "Session label"})

(defn date-range
  [start end]
  {:trap-station-session-start-date start
   :trap-station-session-end-date end})

(defn valid-session-date?
  [session date]
  (sut/valid-session-date? (trap-station-session/trap-station-session
                              (merge default-trap-station-session
                                     session))
                             date))

(deftest test-valid-session-date?
  (testing "valid-session-date?"
    (testing "should return true if date is nil."
      (is (not (valid-session-date? {} nil))))

    (testing "should return true if date is between session start and end."
      (is (valid-session-date? (date-range (t/date-time 2016 11 1)
                                           (t/date-time 2016 12 1))
           (t/date-time 2016 11 10))))

    (testing "should return true if date is the same day, but after, the session end."
      (is (valid-session-date? (date-range (t/date-time 2016 11 1)
                                           (t/date-time 2016 12 1))
                               (t/date-time 2016 12 1 23 59))))

    (testing "should return true if date is exactly the same as the session start."
      (is (valid-session-date? (date-range (t/date-time 2016 11 1)
                                           (t/date-time 2016 12 1))
                               (t/date-time 2016 11 1))))

    (testing "should return false if date is a day following the session end."
      (is (not (valid-session-date? (date-range (t/date-time 2016 11 1)
                                                (t/date-time 2016 12 1))
                                    (t/date-time 2016 12 2 1)))))

    (testing "should return false if date is before the start date."
      (is (not (valid-session-date? (date-range (t/date-time 2016 11 2)
                                                (t/date-time 2016 12 1))
                                    (t/date-time 2016 11 1 23 59)))))

    (testing "should return true if date is exactly 24 hours of the session end."
      (is (valid-session-date? (date-range (t/date-time 2016 11 1)
                                           (t/date-time 2016 12 1 12))
                               (t/date-time 2016 12 2 12 00))))

    (testing "should return false if date is after 24 hours of the session end."
      (is (not (valid-session-date? (date-range (t/date-time 2016 11 1)
                                                (t/date-time 2016 12 1 12))
                                    (t/date-time 2016 12 2 12 0 0 1)))))))
