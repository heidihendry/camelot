(ns camelot.import.datatype-test
  (:require [camelot.import.datatype :as sut]
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

    (testing "understands ISO date format"
      (is (= (sut/as-datetime "2016-04-21T21:57:45.000Z")
             (t/date-time 2016 4 21 21 57 45))))

    (testing "accepts nil"
      (is (= (sut/as-datetime nil) nil)))

    (testing "accepts empty string"
      (is (= (sut/as-datetime "") nil))))

  (testing "could-be-timestamp?"
    (testing "understands yyyy/mm/dd HH:MM:SS"
      (is (sut/could-be-timestamp? "2014/02/01 01:09:02")))

    (testing "understands yyyy/m/d H:M:S"
      (is (sut/could-be-timestamp? "2014/2/1 1:9:2")))

    (testing "understands yyyy:mm:dd HH:MM:SS"
      (is (sut/could-be-timestamp? "2014:03:01 01:09:02")))

    (testing "understands E MMM d H:m:s Z yyyy"
      (is (sut/could-be-timestamp? "Thu Apr 21 21:57:45 +10:00 2016")))

    (testing "understands ISO date format"
      (is (sut/could-be-timestamp? "2016-04-21T21:57:45.000Z")))

    (testing "accepts nil"
      (is (sut/could-be-timestamp? nil)))

    (testing "accepts empty string"
      (is (sut/could-be-timestamp? "")))

    (testing "rejects other values"
      (is (not (sut/could-be-timestamp? "100"))))

    (testing "rejects things which vaguely look like timestamps"
      (is (not (sut/could-be-timestamp? "2000-9a-27T10:11:59"))))

    (testing "rejects dates"
      (is (not (sut/could-be-timestamp? "2014/02/01"))))))

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
             (t/date-time 2014 2 1))))

    (testing "understands ISO date format"
      (is (= (sut/as-date "2016-04-21T00:00:00.000Z")
             (t/date-time 2016 4 21))))

    (testing "understands YYYY-DDD"
      (is (= (sut/as-date "2016-112")
             (t/date-time 2016 4 21)))))

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
      (is (sut/could-be-number? "1")))

    (testing "zero is a number"
      (is (sut/could-be-number? "0")))

    (testing "negative numbers are numbers"
      (is (sut/could-be-number? "-9")))

    (testing "blank values could be a number"
      (is (sut/could-be-number? "")))

    (testing "nil could be a number"
      (is (sut/could-be-number? nil)))

    (testing "numbers can be numerous digits"
      (is (sut/could-be-number? "-919")))

    (testing "numbers can have floating points"
      (is (sut/could-be-number? "-919.03")))

    (testing "things with letters are not numbers"
      (is (not (sut/could-be-number? "-91a9.03"))))))

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

    (testing "blank values could be an integer"
      (is (sut/could-be-integer? "")))

    (testing "nil could be an integer"
      (is (sut/could-be-integer? nil)))

    (testing "integers cannot have floating points"
      (is (= (sut/could-be-integer? "-919.03") false)))

    (testing "things with letters are not integers"
      (is (= (sut/could-be-integer? "-91a9.03") false))))

  (testing "could-be-readable-integer?"
    (testing "integers return true"
      (is (sut/could-be-readable-integer? "1")))

    (testing "integers cannot have floating points"
      (is (not (sut/could-be-integer? "-919.03"))))

    (testing "empty string could be a readable integer"
      (is (sut/could-be-readable-integer? "")))

    (testing "nil could be a readable integer"
      (is (sut/could-be-readable-integer? nil)))

    (testing "units following integer is an integer"
      (is (sut/could-be-readable-integer? "200 pixels")))

    (testing "Invalid syntax is not an integer"
      (is (not (sut/could-be-readable-integer? "{[}"))))))

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
      (is (= (sut/could-be-boolean? "nah") false))))

  (testing "could-be-zero-one?"
    (testing "empty string could be a zero or one boolean"
      (is (sut/could-be-zero-one? "")))

    (testing "nil could be a zero or one boolean"
      (is (sut/could-be-zero-one? nil)))

    (testing "1 could be a zero or one boolean"
      (is (sut/could-be-zero-one? "1")))

    (testing "0 could be a zero or one boolean"
      (is (sut/could-be-zero-one? "0")))

    (testing "true cannot be a zero or one boolean"
      (is (not (sut/could-be-zero-one? "true")))))

  (testing "could-be-true-false?"
    (testing "empty string could be a true/false boolean"
      (is (sut/could-be-true-false? "")))

    (testing "nil could be a true/false boolean"
      (is (sut/could-be-true-false? nil)))

    (testing "true could be a true/false boolean"
      (is (sut/could-be-true-false? "true")))

    (testing "false could be a true/false boolean"
      (is (sut/could-be-true-false? "false")))

    (testing "0 cannot be a true/false boolean"
      (is (not (sut/could-be-true-false? "0")))))

  (testing "could-be-yes-no?"
    (testing "empty string could be a yes/no boolean"
      (is (sut/could-be-yes-no? "")))

    (testing "nil could be a yes/no boolean"
      (is (sut/could-be-yes-no? nil)))

    (testing "no could be a yes/no boolean"
      (is (sut/could-be-yes-no? "no")))

    (testing "yes could be a yes/no boolean"
      (is (sut/could-be-yes-no? "yes")))

    (testing "0 cannot be a zero or one boolean"
      (is (not (sut/could-be-yes-no? "0"))))))

(deftest test-latitude
  (testing "could-be-latitude?"
    (testing "zero is a valid latitude"
      (is (sut/could-be-latitude? "0")))

    (testing "90 is a valid latitude"
      (is (sut/could-be-latitude? "90")))

    (testing "-90 is a valid latitude"
      (is (sut/could-be-latitude? "-90")))

    (testing "-91 is not a valid latitude"
      (is (not (sut/could-be-latitude? "-91"))))

    (testing "-90.3 is not a valid latitude"
      (is (not (sut/could-be-latitude? "-90.3"))))

    (testing "-70.3 is a valid latitude"
      (is (sut/could-be-latitude? "-70.3")))

    (testing "nil is a valid latitude"
      (is (sut/could-be-latitude? nil)))

    (testing "an IP address is not a valid latitude"
      (is (not (sut/could-be-latitude? "127.0.0.1"))))))

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
      (is (= (sut/could-be-longitude? nil) true)))

    (testing "an IP address is not a valid longitude"
      (is (not (sut/could-be-longitude? "127.0.0.1"))))))

(deftest test-file
  (testing "could-be-file?"
    (testing "is a file if it exists, is readable, and is a file"
      (with-redefs [camelot.util.file/exists? (constantly true)
                    camelot.util.file/readable? (constantly true)
                    camelot.util.file/file? (constantly true)]
        (is (sut/could-be-file? "/path/to/file"))))

    (testing "is not a file if it exists, is readable, and but is not a file"
      (with-redefs [camelot.util.file/exists? (constantly true)
                    camelot.util.file/readable? (constantly true)
                    camelot.util.file/file? (constantly false)]
        (is (not (sut/could-be-file? "/path/to/file")))))

    (testing "is not a file if it exists, is not readable, and is a file"
      (with-redefs [camelot.util.file/exists? (constantly true)
                    camelot.util.file/readable? (constantly false)
                    camelot.util.file/file? (constantly true)]
        (is (not (sut/could-be-file? "/path/to/file")))))

    (testing "is not a file if it does not exist"
      (with-redefs [camelot.util.file/exists? (constantly false)]
        (is (not (sut/could-be-file? "/path/to/file")))))))

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

(deftest test-possible-datatypes
  (testing "possible-datatypes"
    (testing "any set of entries should be able to be a string"
      (is (= (sut/possible-datatypes ["abc" "123" "true"])
             #{:string})))

    (testing "a series of numbers could represent numeric datatypes or a string"
      (is (= (sut/possible-datatypes ["122" "123" "59"])
             #{:string :number :integer :readable-integer :longitude})))

    (testing "series of yes/no responses could be a boolean or string"
      (is (= (sut/possible-datatypes ["y" "n" "yes" "No"])
             #{:string :boolean})))

    (testing "nil and blank string can exist without affecting result"
      (is (= (sut/possible-datatypes ["y" "n" nil "yes" nil "No"])
             #{:string :boolean})))))

(deftest test-possible-constraints
  (testing "possible-constraints"
    (testing "any series without nil or blank values can be required"
      (is (= (sut/possible-constraints ["abc" "123" "true"])
             #{:required})))

    (testing "a series with a nil value cannot be required"
      (is (= (sut/possible-constraints ["122" "123" nil "59"])
             #{})))

    (testing "a series with a blank value cannot be required"
      (is (= (sut/possible-constraints ["abc" "123" "true" ""])
             #{})))))

(deftest test-deserialise
  (testing "deserialise"
    (testing "should deserialise a string field to a string"
      (is (= (sut/deserialise {:field {:datatype :string}} :field "str")
             "str")))

    (testing "should deserialise a string field to a string"
      (is (= (sut/deserialise :taxonomy-common-name "smiley wolf")
             "smiley wolf")))

    (testing "should deserialise a timestamp field to a date-time"
      (is (= (sut/deserialise :media-capture-timestamp "2016-04-21 10:10:10")
             (t/date-time 2016 4 21 10 10 10))))

    (testing "should return nil if given a field for which it does not have a schema"
      (is (nil? (sut/deserialise :bad-field "2016-04-21 10:10:10"))))))
