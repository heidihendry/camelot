(ns camelot.processing.rename-photo
  (:require [schema.core :as s]
            [camelot.processing.photo :as photo]
            [clj-time.format :as tf]
            [taoensso.tower :as tower]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [camelot.util.java-file :as f])
  (:import [org.apache.commons.io FilenameUtils]
           [java.io File]))

(s/defn stringify-field :- s/Str
  "Return a given metadata field as a string."
  [config metadata extr]
  {:pre [(not (nil? extr))]}
  (let [time-fmt (tf/formatter (or (:date-format (:rename config))
                                   "YYYY-MM-dd HH.mm.ss"))]
    (cond
      (instance? org.joda.time.DateTime extr) (tf/unparse time-fmt extr)
      (instance? java.lang.Long extr) (str extr)
      true extr)))

(s/defn extract-all-fields :- [s/Str]
  "Return a list of all fields needed for photo renaming.
Throw IllegalStateException should fields not be present in the metadata."
  [state metadata]
  (let [fields (:fields (:rename (:config state)))
        extract (map (partial photo/extract-path-value metadata) fields)
        problems (reduce-kv (fn [acc i v]
                              (if (nil? v)
                                (conj acc (get fields i))
                                acc))
                            [] (into [] extract))]
    (if (empty? problems)
      (map #(stringify-field (:config state) metadata %) extract)
      (throw (java.lang.IllegalStateException.
              (str ((:translate state) :problems/error)
                   ((:translate state) :problems/rename-field-not-found problems)))))))

(defn create-photo-name
  "Return a file's desired basename (sans extension) as a string."
  [state metadata]
  (let [fmt (:format (:rename (:config state)))
        fd (extract-all-fields state metadata)]
    (apply format fmt fd)))

(defn- generate-rename-actions
  "Return a pair of source and destination filenames.
Should renaming not be possible, return nil."
  [state [file metadata]]
  (let [new-name (create-photo-name state metadata)
        dir (f/get-parent file)
        extension (FilenameUtils/getExtension (f/get-name file))
        basename (FilenameUtils/removeExtension (f/get-name file))
        new-file (io/file (str dir "/" new-name "." (str/lower-case extension)))]
    (when (not (= file new-file))
      (if (f/exists? new-file)
        (println (str ((:translate state) :problems/warn)
                      ((:translate state) :problems/rename-existing-conflict
                       (f/to-path file) (f/to-path new-file))))
        [file new-file]))))

(defn- rename-reducer
  "Reducer for verifying uniqueness of the desired filenames."
  [acc [f t]]
  (let [v (get acc t)]
    (if (nil? v)
      (assoc acc t {:count 1, :sources [f]})
      (assoc acc t {:count (inc (get v :count))
                    :sources (conj (get v :sources) f)}))))

(defn- validate-renames
  "Return all safe renames.
If any rename may be conflicting, none of the renames are performed."
  [state renames]
  (let [chkdata (reduce rename-reducer {} renames)
        chkfail (filter (fn [[k v]] (when (> (:count v) 1)
                                      true))
                        chkdata)]
    (if (empty? chkfail)
      renames
      (do (doseq [[k {fs :sources}] chkfail]
            (println (str ((:translate state) :problems/rename-conflict (f/get-path k))))
            (doseq [f fs] (println "  *" (f/get-path f))))
          '()))))

(defn apply-renames
  "Reducer for updating photo filenames after modification."
  [album renames]
  (reduce (fn [alb [from to]]
            (dissoc (assoc alb to (get alb from)) from))
          album renames))

(defn rename-photos
  "Rename all photos in the given album."
  [state [dir album]]
  (println ((:translate state) :status/rename-photos (f/get-path dir)))
  (let [renames (->> (:photos album)
                     (map #(generate-rename-actions state %))
                     (remove nil?)
                     (validate-renames state))]
    (doseq [[from to] renames]
      (println ((:translate state) :status/apply-rename (f/get-path from) (f/get-path to)))
      (f/rename-to from to))
    {dir (assoc album :photos (apply-renames (:photos album) renames))}))