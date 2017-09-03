(ns camelot.translation.core
  (:require [taoensso.tower :as tower :refer-macros [with-tscope dict-compile*]]
            [camelot.state :as state]
            [clojure.string :as string]
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

(defn- long-list-to-user-string
  [l]
  (let [rl (reverse l)]
    (->> rl
         rest
         (cons (gstr/format "%s %s" (translate :words/and-lc) (first rl)))
         reverse
         (string/join ", "))))

(defn list-to-user-string
  [l]
  (case (count l)
    0 nil
    1 (first l)
    2 (gstr/format "%s %s %s" (first l) (translate :words/and-lc) (second l))
    (long-list-to-user-string l)))
