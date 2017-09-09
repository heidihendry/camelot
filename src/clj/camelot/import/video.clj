(ns camelot.import.video
  (:require
   [camelot.import.store :as store]
   [clj-time.format :as tf]
   [clj-time.local :as tl]
   [camelot.util.file :as util.file]
   [camelot.import.ocr :as ocr]
   [clojure.string :as string]
   [clojure.java.io :as io])
  (:import
   (javax.imageio ImageIO)
   (java.io File)
   (java.awt.image BufferedImage)
   (org.bytedeco.javacv FFmpegFrameGrabber)
   (org.bytedeco.javacv Java2DFrameConverter)
   (org.jcodec.api.awt AWTFrameGrab)))

(defn get-thumb-frame ^BufferedImage
  [^String file]
  (let [g (org.bytedeco.javacv.FFmpegFrameGrabber. file)]
    (.start g)
    (.setTimestamp g (long (/ (.getLengthInTime g) 2)))
    (let [img (.grab g)
          converter (Java2DFrameConverter.)]
      (.stop g)
      (.getBufferedImage converter img))))

(defn get-frame
  [^String file n]
  (let [g (org.bytedeco.javacv.FFmpegFrameGrabber. file)]
    (.start g)
    (.setFrameNumber g n)
    (let [img (.grab g)
          converter (Java2DFrameConverter.)]
      (.stop g)
      (.getBufferedImage converter img))))

(defn crop
  [^BufferedImage buf-img x y w h]
  (.getSubimage buf-img x y w h))

(defn timestamp-image
  [camera file]
  (let [img (crop (get-frame file (:frame camera))
                  (:crop-x camera)
                  (:crop-y camera)
                  (:crop-width camera)
                  (:crop-height camera))]
    (when (:debug camera)
      (let [ts (tl/format-local-time (tl/local-now) :basic-date-time-no-ms)
            fname (str "cam-debug-" ts ".png")]
        (ImageIO/write ^BufferedImage img "png" (io/file fname))))
    img))

(defn get-timestamp
  [camera file]
  (tf/parse (tf/formatter (:date-format camera))
            (string/trim (ocr/tess (timestamp-image camera (util.file/get-path file))))))
