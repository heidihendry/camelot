(ns camelot.bulk-import.datatype-test
  (:require [camelot.bulk-import.datatype :as sut]
            [clojure.test :refer :all]
            [clj-time.core :as t]))

(deftest test-datetime
  (testing "as-datetime"
    (testing "understands yyyy/mm/dd HH:MM:SS"
      (is (= (sut/as-datetime "2014/02/01 01:09:02")
             (t/date-time 2014 2 1 1 9 2))))

    (testing "understands yyyy/m/d H:M:S"
      (is (= (sut/as-datetime "2014/2/1 1:9:2")
             (t/date-time 2014 2 1 1 9 2))))

    (testing "understands yyyy:mm:dd HH:MM:SS"
      (is (= (sut/as-datetime "2014:03:01 01:09:02")
             (t/date-time 2014 3 1 1 9 2))))

    (testing "understands E MMM d H:m:s Z yyyy"
      (is (= (sut/as-datetime "Thu Apr 21 21:57:45 +10:00 2016")
             (t/date-time 2016 4 21 11 57 45))))

    (testing "accepts nil"
      (is (= (sut/as-datetime nil) nil)))

    (testing "accepts empty string"
      (is (= (sut/as-datetime "") nil))))

  (testing "could-be-timestamp?"
    (testing "understands yyyy/mm/dd HH:MM:SS"
      (is (= (sut/could-be-timestamp? "2014/02/01 01:09:02") true)))

    (testing "understands yyyy/m/d H:M:S"
      (is (= (sut/could-be-timestamp? "2014/2/1 1:9:2") true)))

    (testing "understands yyyy:mm:dd HH:MM:SS"
      (is (= (sut/could-be-timestamp? "2014:03:01 01:09:02") true)))

    (testing "understands E MMM d H:m:s Z yyyy"
      (is (= (sut/could-be-timestamp? "Thu Apr 21 21:57:45 +10:00 2016") true)))

    (testing "accepts nil"
      (is (= (sut/could-be-timestamp? nil) true)))

    (testing "accepts empty string"
      (is (= (sut/could-be-timestamp? "") true)))

    (testing "rejects other values"
      (is (= (sut/could-be-timestamp? "100") false)))

    (testing "rejects dates"
      (is (= (sut/could-be-timestamp? "2014/02/01") false)))))

(deftest test-date
  (testing "as-date"
    (testing "understands yyyy/mm/dd HH:MM:SS"
      (is (= (sut/as-date "2014/02/01 01:09:02")
             (t/date-time 2014 2 1 1 9 2))))

    (testing "understands yyyy/mm/dd"
      (is (= (sut/as-date "2014/02/01")
             (t/date-time 2014 2 1))))

    (testing "understands yyyy-mm-dd"
      (is (= (sut/as-date "2014-02-01")
             (t/date-time 2014 2 1))))

    (testing "understands yyyy:mm:dd"
      (is (= (sut/as-date "2014:02:01")
             (t/date-time 2014 2 1)))))

  (testing "could-be-date?"
    (testing "understands yyyy/mm/dd HH:MM:SS"
      (is (= (sut/could-be-date? "2014/02/01 01:09:02") true)))

    (testing "understands yyyy/mm/dd"
      (is (= (sut/could-be-date? "2014/02/01") true)))

    (testing "understands yyyy-mm-dd"
      (is (= (sut/could-be-date? "2014-02-01") true)))

    (testing "understands yyyy:mm:dd"
      (is (= (sut/could-be-date? "2014:02:01") true)))

    (testing "rejects other values"
      (is (= (sut/could-be-date? "100") false)))))

(deftest test-number
  (testing "could-be-number?"
    (testing "numbers return true"
      (is (= (sut/could-be-number? "1") true)))

    (testing "zero is a number"
      (is (= (sut/could-be-number? "0") true)))

    (testing "negative numbers are numbers"
      (is (= (sut/could-be-number? "-9") true)))

    (testing "numbers can be numerous digits"
      (is (= (sut/could-be-number? "-919") true)))

    (testing "numbers can have floating points"
      (is (= (sut/could-be-number? "-919.03") true)))

    (testing "things with letters are not numbers"
      (is (= (sut/could-be-number? "-91a9.03") false)))))

(deftest test-integer
  (testing "could-be-integer?"
    (testing "integers return true"
      (is (= (sut/could-be-integer? "1") true)))

    (testing "zero is a integer"
      (is (= (sut/could-be-integer? "0") true)))

    (testing "negative integers are integers"
      (is (= (sut/could-be-integer? "-9") true)))

    (testing "integers can be numerous digits"
      (is (= (sut/could-be-integer? "-919") true)))

    (testing "integers cannot have floating points"
      (is (= (sut/could-be-integer? "-919.03") false)))

    (testing "things with letters are not integers"
      (is (= (sut/could-be-integer? "-91a9.03") false)))))

(deftest test-boolean
  (testing "as-boolean"
    (testing "1/0 is a boolean pair, and 1 is true"
      (is (= (sut/as-boolean "1") true)))

    (testing "1/0 is a boolean pair, and 0 is false"
      (is (= (sut/as-boolean "0") false)))

    (testing "yes/no is a boolean pair, and yes is true"
      (is (= (sut/as-boolean "yes") true)))

    (testing "yes/no is a boolean pair, and YeS is true"
      (is (= (sut/as-boolean "YeS") true)))

    (testing "yes/no is a boolean pair, and no is false"
      (is (= (sut/as-boolean "no") false)))

    (testing "yes/no is a boolean pair, and No is false"
      (is (= (sut/as-boolean "No") false)))

    (testing "Y/N is a boolean pair, and y is true"
      (is (= (sut/as-boolean "y") true)))

    (testing "Y/N is a boolean pair, and N is false"
      (is (= (sut/as-boolean "N") false)))

    (testing "true/false is a boolean pair, and True is true"
      (is (= (sut/as-boolean "true") true)))

    (testing "true/false is a boolean pair, and TrUe is true"
      (is (= (sut/as-boolean "TrUe") true)))

    (testing "true/false is a boolean pair, and false is false"
      (is (= (sut/as-boolean "false") false)))

    (testing "true/false is a boolean pair, and FalSE is false"
      (is (= (sut/as-boolean "FalSe") false)))

    (testing "T/F is a boolean pair, and T is true"
      (is (= (sut/as-boolean "T") true)))

    (testing "T/F is a boolean pair, and F is true"
      (is (= (sut/as-boolean "f") false)))

    (testing "nil is false"
      (is (= (sut/as-boolean nil) false)))

    (testing "empty string is false"
      (is (= (sut/as-boolean "") false))))

  (testing "could-be-boolean?"
    (testing "1/0 is a boolean pair"
      (is (= (sut/could-be-boolean? "1") true))
      (is (= (sut/could-be-boolean? "0") true)))

    (testing "yes/no is a boolean pair, and yes is true"
      (is (= (sut/could-be-boolean? "yes") true))
      (is (= (sut/could-be-boolean? "YeS") true))
      (is (= (sut/could-be-boolean? "no") true))
      (is (= (sut/could-be-boolean? "No") true)))

    (testing "Y/N is a boolean pair, and y is true"
      (is (= (sut/could-be-boolean? "y") true))
      (is (= (sut/could-be-boolean? "N") true)))

    (testing "true/false is a boolean pair"
      (is (= (sut/could-be-boolean? "true") true))
      (is (= (sut/could-be-boolean? "TrUe") true))
      (is (= (sut/could-be-boolean? "false") true))
      (is (= (sut/could-be-boolean? "FalSe") true)))

    (testing "T/F is a boolean pair"
      (is (= (sut/could-be-boolean? "T") true))
      (is (= (sut/could-be-boolean? "t") true))
      (is (= (sut/could-be-boolean? "F") true))
      (is (= (sut/could-be-boolean? "f") true)))

    (testing "nil could be a boolean true"
      (is (= (sut/could-be-boolean? nil) true)))

    (testing "empty string could be a boolean"
      (is (= (sut/could-be-boolean? "") true)))

    (testing "other strings cannot be be a boolean"
      (is (= (sut/could-be-boolean? "nah") false)))))

(deftest test-latitude
  (testing "could-be-latitude?"
    (testing "zero is a valid latitude"
      (is (= (sut/could-be-latitude? "0") true)))

    (testing "90 is a valid latitude"
      (is (= (sut/could-be-latitude? "90") true)))

    (testing "-90 is a valid latitude"
      (is (= (sut/could-be-latitude? "-90") true)))

    (testing "-91 is not a valid latitude"
      (is (= (sut/could-be-latitude? "-91") false)))

    (testing "-90.3 is not a valid latitude"
      (is (= (sut/could-be-latitude? "-90.3") false)))

    (testing "-70.3 is a valid latitude"
      (is (= (sut/could-be-latitude? "-70.3") true)))

    (testing "nil is a valid latitude"
      (is (= (sut/could-be-latitude? nil) true)))))

(deftest test-longitude
  (testing "could-be-longitude?"
    (testing "zero is a valid longitude"
      (is (= (sut/could-be-longitude? "0") true)))

    (testing "180 is a valid longitude"
      (is (= (sut/could-be-longitude? "180") true)))

    (testing "-180 is a valid longitude"
      (is (= (sut/could-be-longitude? "-180") true)))

    (testing "-181 is not a valid longitude"
      (is (= (sut/could-be-longitude? "-181") false)))

    (testing "-180.3 is not a valid longitude"
      (is (= (sut/could-be-longitude? "-180.3") false)))

    (testing "-170.3 is a valid longitude"
      (is (= (sut/could-be-longitude? "-170.3") true)))

    (testing "nil is a valid longitude"
      (is (= (sut/could-be-longitude? nil) true)))))

(deftest test-sex
  (testing "as-sex"
    (testing "A sex can be male"
      (is (= (sut/as-sex "M") "M"))
      (is (= (sut/as-sex "Male") "M"))
      (is (= (sut/as-sex "male") "M")))

    (testing "A sex can be female"
      (is (= (sut/as-sex "F") "F"))
      (is (= (sut/as-sex "Female") "F"))
      (is (= (sut/as-sex "female") "F")))

    (testing "nil is valid"
      (is (= (sut/as-sex nil) nil))
      (is (= (sut/as-sex "") nil))))

  (testing "could-be-sex?"
    (testing "A sex can be male"
      (is (= (sut/could-be-sex? "M") true))
      (is (= (sut/could-be-sex? "Male") true))
      (is (= (sut/could-be-sex? "male") true)))

    (testing "A sex can be female"
      (is (= (sut/could-be-sex? "F") true))
      (is (= (sut/could-be-sex? "Female") true))
      (is (= (sut/could-be-sex? "female") true)))

    (testing "nil and blank are allowed"
      (is (= (sut/could-be-sex? nil) true))
      (is (= (sut/could-be-sex? "") true)))

    (testing "other values are not allowed"
      (is (= (sut/could-be-sex? "adult") false)))))

(deftest test-lifestage
  (testing "as-lifestage"
    (testing "A lifestage can be adult"
      (is (= (sut/as-lifestage "A") "adult"))
      (is (= (sut/as-lifestage "Adult") "adult"))
      (is (= (sut/as-lifestage "adult") "adult")))

    (testing "A lifestage can be juvenile"
      (is (= (sut/as-lifestage "J") "juvenile"))
      (is (= (sut/as-lifestage "Juvenile") "juvenile"))
      (is (= (sut/as-lifestage "juvenile") "juvenile")))

    (testing "nil is valid"
      (is (= (sut/as-lifestage nil) nil))
      (is (= (sut/as-lifestage "") nil))))

  (testing "could-be-lifestage?"
    (testing "A lifestage can be adult"
      (is (= (sut/could-be-lifestage? "A") true))
      (is (= (sut/could-be-lifestage? "Adult") true))
      (is (= (sut/could-be-lifestage? "adult") true)))

    (testing "A lifestage can be juvenile"
      (is (= (sut/could-be-lifestage? "J") true))
      (is (= (sut/could-be-lifestage? "Juvenile") true))
      (is (= (sut/could-be-lifestage? "juvenile") true)))

    (testing "nil and blank are allowed"
      (is (= (sut/could-be-lifestage? nil) true))
      (is (= (sut/could-be-lifestage? "") true)))

    (testing "other values are not allowed"
      (is (= (sut/could-be-lifestage? "male") false)))))

(deftest test-required-constraint
  (testing "could-be-required?"
    (testing "A present value can be required"
      (is (= (sut/could-be-required? "A") true))
      (is (= (sut/could-be-required? "0") true)))

    (testing "A blank or nil value cannot be required"
      (is (= (sut/could-be-required? nil) false))
      (is (= (sut/could-be-required? "") false)))))
