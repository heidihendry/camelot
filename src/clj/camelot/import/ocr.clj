(ns camelot.import.ocr
  (:require
   [clj-time.format :as tf])
  (:import
   (org.bytedeco.javacpp tesseract$TessBaseAPI)
   (org.bytedeco.javacpp lept)
   (java.awt.image BufferedImage)
   (java.io ByteArrayOutputStream)
   (javax.imageio ImageIO)))

(defn tess
  [buffered-image]
  (let [api (tesseract$TessBaseAPI.)
        imgdata (ByteArrayOutputStream.)]
    (ImageIO/write ^BufferedImage buffered-image "png" imgdata)
    (if (not= (.Init api "./" "eng") 0)
      (throw (RuntimeException. "Could not find Eng Tesseract model.")))
    (let [ba (.toByteArray imgdata)
          img (lept/pixReadMemPng ba (alength ba))]
      (.SetImage api img)
      (.getString (.GetUTF8Text api)))))
