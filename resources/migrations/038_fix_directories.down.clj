(require '[camelot.system.state :as state])
(require '[camelot.util.file :as file])

(let [media-dir (file/->file (:media (state/path-map)))]
  (dorun (map
          (fn [f]
            (when (and (file/file? f)
                       (file/readable? f)
                       (not= (file/get-parent-file f) media-dir))
              (file/rename f (file/->file media-dir (file/get-name f)))))
          (file-seq media-dir)))
  (dorun (map
          (fn [f]
            (when (and (file/directory? f) (= (count (file/get-name f)) 2))
              (file/delete f)))
          (file-seq media-dir))))
