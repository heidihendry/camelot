(require '[camelot.util.state :as state])
(require '[camelot.util.file :as file])

(defn- -m038-downgrade
  [state]
  (let [media-dir (state/lookup-path state :media)]
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
            (file-seq media-dir)))))

(-m038-downgrade camelot.system.db.core/*migration-state*)
