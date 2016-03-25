(ns ctdp.action.rename-photo
  (:require [schema.core :as s]
            [clj-time.format :as tf]
            [taoensso.tower :as tower]
            [clojure.java.io :as io]
            [clojure.string :as str])
  (:import [org.apache.commons.io FilenameUtils]))

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
                                acc)) [] (into [] extract))]
    (if (empty? problems)
      (map #(stringify-field (:config state) metadata %) extract)
      (throw (java.lang.IllegalStateException.
              (str ((:translate state) :problems/error)
                   ((:translate state) :problems/rename-field-not-found problems)))))))

(defn create-photo-name
  [state metadata]
  (let [fmt (:format (:rename (:config state)))
        fd (extract-all-fields state metadata)]
    (apply format fmt fd)))

(defn- generate-rename-actions
  [state [file metadata]]
  (let [new-name (create-photo-name state metadata)
        dir (.getParent file)
        extension (FilenameUtils/getExtension (.getName file))
        basename (FilenameUtils/removeExtension (.getName file))
        new-file (io/file (str dir "/" new-name "." (str/lower-case extension)))]
    (when (not (= file new-file))
      (if (.exists new-file)
        (println (str ((:translate state) :problems/warn)
                      ((:translate state) :problems/rename-existing-conflict
                       (.toPath file) (.toPath new-file))))
        [file new-file]))))

(defn- rename-reducer
  [acc [f t]]
  (let [v (get acc t)]
    (if (nil? v)
      (assoc acc t {:count 1, :sources [f]})
      (assoc acc t {:count (inc (get v :count))
                    :sources (conj (get v :sources) f)}))))

(defn- validate-renames
  [state renames]
  (let [chkdata (reduce rename-reducer {} renames)
        chkfail (filter (fn [[k v]] (when (> (:count v) 1)
                                      true))
                        chkdata)]
    (if (empty? chkfail)
      renames
      (do (doseq [[k {fs :sources}] chkfail]
            (println (str ((:translate state) :problems/rename-conflict (.getPath k))))
            (doseq [f fs] (println "  *" (.getPath f))))
          '()))))

(defn rename-photos
  [state [dir album]]
  (println ((:translate state) :status/rename-photos (.getPath dir)))
  (let [renames (->> (:photos album)
                     (map #(generate-rename-actions state %))
                     (remove nil?)
                     (validate-renames state))]
    (doseq [[from to] renames]
      (println ((:translate state) :status/apply-rename (.getPath from) (.getPath to)))
      (.renameTo from to))))
