(ns camelot.import.image
  (:require
   [camelot.util.config :as config]
   [camelot.util.file :as file]
   [clojure.java.io :as io]
   [clojure.string :as str]
   [mikera.image.core :as image]
   [clojure.tools.logging :as log])
  (:import
   (org.apache.commons.lang3 SystemUtils)
   (java.util UUID)
   (java.io IOException)
   (java.awt.image BufferedImage)))

(defn- save-pathname
  [f dest]
  (io/make-parents dest)
  (let [d (file/->file dest)]
    (if (file/exists? d)
      (throw (IOException. (format "copy-pathname: file '%s' already exists", dest)))
      (f dest))))

(def image-variants
  {"thumb-" 256
   "" nil})

(defn- store-original
  [src dest]
  (save-pathname #(io/copy (file/->file src) (file/->file %)) dest))

(defn- store-variant
  [^BufferedImage image dest]
  (save-pathname #(image/save image % :quality 0.7 :progressive false) dest))

(defn- create-variant
  [path target width]
  (let [img (image/load-image path)]
    (store-variant (image/resize img width) target)))

(defn- create-image
  [state path file-basename extension variant width]
  (let [fdir (io/file (get-in state [:config :path :media])
                      (subs (str/lower-case file-basename) 0 2))]
    (when-not (file/exists? fdir)
      (file/mkdir fdir))
    (let [target (file/->file fdir
                              (str variant
                                   (str/lower-case file-basename)))]
      (if width
        ;; Always create variants as .png; OpenJDK cannot write .jpg
        (create-variant path (str (file/get-path target) ".png") width)
        (store-original path (str (file/get-path target) "." extension))))))

(defn create-image-files
  [state path extension]
  (let [filename (str/lower-case (UUID/randomUUID))]
    (dorun (map (fn [[k v]] (create-image state path filename extension k v))
                image-variants))
    filename))

(defn add-media-file!
  [state record]
  (let [fmt (str/lower-case (second (re-find #".*\.(.+?)$" (file/get-name (:absolute-path record)))))
        filename (create-image-files state (:absolute-path record) fmt)]
    (merge {:media-filename filename
            :media-format fmt
            :media-cameracheck false
            :media-attention-needed false}
           record)))
