(require '[camelot.util.state :as state])
(require '[camelot.util.file :as file])
(require '[clojure.string :as str])

(let [media-dir (get-in (state/read-config) [:paths :media])
      mfiles (file/list-files media-dir)]
  (dorun (map
          (fn [f]
            (let [prefix-dir (file/->file media-dir (apply str (take 2 (seq (str/replace (file/get-name f) #"^(thumb|preview)-" "")))))]
              (when-not (file/exists? prefix-dir)
                (file/mkdir prefix-dir))
              (file/rename f (file/->file prefix-dir (file/get-name f)))))
          mfiles)))
