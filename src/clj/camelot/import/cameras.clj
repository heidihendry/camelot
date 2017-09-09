(ns camelot.import.cameras)

(def cameras
  {:cuddeback-default
   {:date-format "MM/d/yyyy h:m a"
    :debug true
    :frame 1
    :crop-x 400
    :crop-y 460
    :crop-width 450
    :crop-height 70}})

(def camera-models
  {:cuddeback {:default (:cuddeback-default cameras)}})
