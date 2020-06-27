(ns camelot.protocols)

(defprotocol Learnable
  (learn [this thing]
    "Learn about the given `thing`"))
