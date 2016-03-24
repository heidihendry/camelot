(ns ctdp.action.rename-photo
  (:require [ctdp.translations.core :refer :all]
            [schema.core :as s]
            [clj-time.format :as tf]
            [taoensso.tower :as tower]))

(s/defn stringify-field :- s/Str
  [config metadata extr]
  {:pre [(not (nil? extr))]}
  (let [time-fmt (tf/formatter (or (:date-format (:rename config))
                                   "YYYY-MM-dd HH.mm.ss"))]
    (cond
      (instance? org.joda.time.DateTime extr) (tf/unparse time-fmt extr)
      (instance? java.lang.Long extr) (str extr)
      true extr)))

(s/defn extract-all-fields :- [s/Str]
  [state metadata]
  (let [fields (:fields (:rename (:config state)))
        extract (map #(reduce (fn [acc n] (get acc n)) metadata %) fields)
        problems (reduce-kv (fn [acc i v]
                              (if (nil? v)
                                (conj acc (get fields i))
                                acc)) [] (into [] extract))
        tlookup #((:translations state) (:language (:config state)) %)]
    (if (empty? problems)
      (map #(stringify-field (:config state) metadata %) extract)
      (throw (java.lang.IllegalStateException.
              ;; TODO i18n
              (str "[" (tlookup :problems/error) "] "
                   (tlookup :problems/rename-field-not-found) "'" problems "'"))))))

(defn create-photo-name
  [state [file metadata]]
  (let [fmt (:format (:rename (:config state)))
        fd (extract-all-fields state metadata)]
    (apply format fmt fd)))

(defn- rename-photo
  [state [file metadata]]
  (let [new-name (create-photo-name)]
    ;; TODO Should preserve extension of original file
    ;; TODO Should check if target file exists
    ))

(defn rename-photos
  [state [dir album]]
  (doall (map #(rename-photo state %) (:photos album))))
