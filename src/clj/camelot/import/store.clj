(ns camelot.import.store
  (:require
   [clojure.java.io :as io]
   [clojure.string :as string]
   [camelot.util.file :as file])
  (:import
   (java.util UUID)))

(defn new-filename
  []
  (string/lower-case (UUID/randomUUID)))

(defn path
  [state file-basename variant]
  (let [fdir (io/file (get-in state [:config :path :media])
                      (subs (string/lower-case file-basename) 0 2))]
    (when-not (file/exists? fdir)
      (file/mkdir fdir))
    (file/->file fdir
                 (str variant
                      (string/lower-case file-basename)))))
