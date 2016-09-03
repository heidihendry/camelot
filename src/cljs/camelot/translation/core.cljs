(ns camelot.translation.core
  (:require [taoensso.tower :as tower :refer-macros (with-tscope dict-compile*)]
            [camelot.state :as state]
            [camelot.translation.en :refer [t-en]]
            [goog.string :as gstr]))

(def tconfig
  "Configuration for translations."
  {:dev-mode? true
   :compiled-dictionary (dict-compile* {:en t-en})
   :fallback-locale :en})

(defn translate
  "Create a translator for the user's preferred language."
  [tkey & vars]
  (let [tlookup (partial (tower/make-t tconfig) (:language (state/app-state-cursor)))]
    (if (seq vars)
      (apply gstr/format (tlookup tkey) vars)
      (tlookup tkey))))
