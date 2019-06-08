(require '[camelot.util.state :as state])
(require '[camelot.util.file :as file])
(require '[clojure.string :as str])

(defn -m038-remove-filename-prefix
  [fname]
  (str/replace fname #"^(thumb|preview)-" ""))

(defn -m038-is-media-file-name?
  [f]
  (->> f
       file/get-name
       -m038-remove-filename-prefix
       (re-matches #"^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\..*$")))

(defn -m038-is-movable-file?
  [f]
  (and (file/file? f)
       (file/readable? f)
       (-m038-is-media-file-name? f)))

(defn -m038-get-leading-chars
  [f]
  (apply str (take 2 (seq (-m038-remove-filename-prefix (file/get-name f))))))

(let [media-dir (get-in (state/read-config) [:paths :media])
      mfiles (file/list-files media-dir)]
  (dorun (->> mfiles
              (filter #(-m038-is-movable-file? %))
              (map
               (fn [f]
                 (let [prefix-dir (file/->file media-dir
                                               (-m038-get-leading-chars f))]
                   (when-not (file/exists? prefix-dir)
                     (file/mkdir prefix-dir))
                   (file/rename f (file/->file prefix-dir
                                               (file/get-name f)))))))))
