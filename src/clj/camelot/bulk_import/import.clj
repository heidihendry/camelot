(ns camelot.bulk-import.import
  (:require
   [camelot.util.config :as config]
   [camelot.util.db :refer [with-transaction]]
   [camelot.import.album :as album]
   [camelot.bulk-import.db :as db]
   [camelot.util.file :as file]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [clojure.core.async :refer [<! chan >!! go-loop close!] :as async]
   [compojure.core :refer [ANY context DELETE GET POST PUT]]
   [mikera.image.core :as image]
   [clojure.tools.logging :as log])
  (:import
   (org.apache.commons.lang3 SystemUtils)))

(defn- save-pathname
  [f dest]
  (io/make-parents dest)
  (let [d (file/->file dest)]
    (if (file/exists? d)
      (throw (java.io.IOException. (format "copy-pathname: file '%s' already exists", dest)))
      (f dest))))

(def image-variants
  {"thumb-" 256
   "" nil})

(defn- store-original
  [src dest]
  (save-pathname #(io/copy (file/->file src) (file/->file %)) dest))

(defn- store-variant
  [^java.awt.image.BufferedImage image dest]
  (save-pathname #(image/save image % :quality 0.7 :progressive false) dest))

(defn- create-variant
  [path target width]
  (let [img (image/load-image path)]
    (store-variant (image/resize img width) target)))

(defn- create-image
  [state path file-basename extension variant width]
  (let [target (str (get-in state [:config :path :media]) SystemUtils/FILE_SEPARATOR
                    variant (str/lower-case file-basename))]
    (if width
      ;; Always create variants as .png; OpenJDK cannot write .jpg
      (create-variant path (str target ".png") width)
      (store-original path (str target "." extension)))))

(defn create-image-files
  [state path extension]
  (let [filename (str/lower-case (java.util.UUID/randomUUID))]
    (dorun (map (fn [[k v]] (create-image state path filename extension k v))
                image-variants))
    filename))

(defn- add-media-file!
  [state record]
  (let [fmt (str/lower-case (second (re-find #".*\.(.+?)$" (file/get-name (:absolute-path record)))))
        filename (create-image-files state (:absolute-path record) fmt)]
    (merge {:media-filename filename
            :media-format fmt
            :media-cameracheck false
            :media-attention-needed false}
           record)))

(defn do-import
  "Import a media file."
  [msg]
  (try
    (with-transaction [s (:state msg)]
      (->> (:record msg)
           (add-media-file! s)
           (db/create-media! s)
           (db/create-sighting! s)
           (db/create-photo! s)))
    (assoc msg :result :complete
           :absolute-path (get-in msg [:record :absolute-path]))
    (catch Exception e
      (log/error (.getMessage e))
      (log/error (str/join "\n" (map str (.getStackTrace e))))
      (assoc msg :result :failed
             :absolute-path (get-in msg [:record :absolute-path])))))

(defn add-import-result
  "Update importer state with the latest result."
  [{:keys [state result absolute-path]}]
  (dosync
   (alter (get-in state [:importer :pending]) dec)
   (alter (get-in state [:importer result]) inc)
   (when (= result :failed)
     (alter (get-in state [:importer :failed-paths]) conj absolute-path))))

(defn media-processor
  "Setup a pipeline for parallel media import and process results."
  [num-importers]
  (let [ch (chan)
        result-chan (chan num-importers)]
    (async/pipeline-blocking num-importers result-chan (map do-import) ch)
    (go-loop []
      (let [msg (<! result-chan)]
        (add-import-result msg))
      (recur))
    ch))

(defn import-media-fn
  "Setup data structure and process media import."
  [num-importers]
  (let [proc (media-processor num-importers)]
    (fn [state record]
      (try
        (>!! proc (with-transaction [s state]
                   (->> record
                        (db/get-survey s)
                        (db/get-or-create-camera! s)
                        (db/get-or-create-site! s)
                        (db/get-or-create-survey-site! s)
                        (db/get-or-create-trap-station! s)
                        (db/get-or-create-trap-session! s)
                        (db/get-or-create-trap-camera! s)
                        (hash-map :state state :record))))
        (catch Exception e
          (log/error (.getMessage e))
          (log/error (str/join "\n" (map str (.getStackTrace e))))
          (add-import-result {:state state
                              :result :failed
                              :absolute-path (:absolute-path record)}))))))
