(ns camelot.translation.core
  (:require [taoensso.tower :as tower :refer-macros [with-tscope dict-compile*]]
            [camelot.state :as state]
            [goog.string :as gstr]))

(def tconfig
  "Configuration for translations."
  {:dev-mode? true
   :compiled-dictionary (dict-compile* (do
                                         (clojure.core/require '[camelot.translation.en])
                                         (clojure.core/require '[camelot.translation.vn])
                                         {:en camelot.translation.en/t-en
                                          :vn camelot.translation.vn/t-vn}))
   :fallback-locale :en})

(defn get-language
  []
  (if (:resources (state/app-state-cursor))
    (get-in (get (state/resources-state)
                 (get-in (state/app-state-cursor) [:view :settings :screen :type]))
            [:language :value])
    :en))

(defn translate
  "Create a translator for the user's preferred language."
  [tkey & vars]
  (let [tlookup (partial (tower/make-t tconfig) (get-language))]
    (if (seq vars)
      (apply gstr/format (tlookup tkey) vars)
      (tlookup tkey))))
