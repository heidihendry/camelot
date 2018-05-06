(ns camelot.import.bulk
  "Machinery for importing media via bulk import."
  (:require
   [camelot.import.image :as image]
   [camelot.import.db :as db]
   [camelot.import.template :as template]
   [camelot.import.validate :as validate]
   [camelot.model.survey :as survey]
   [camelot.translation.core :as tr]
   [camelot.util.datatype :as datatype]
   [camelot.util.db :refer [with-transaction]]
   [camelot.util.model :as model]
   [clojure.core.async :refer [<! >!! chan go-loop] :as async]
   [clojure.string :as str]
   [clojure.tools.logging :as log]))

(defn do-import
  "Import a media file."
  [msg]
  (try
    (with-transaction [s (:state msg)]
      (->> (:record msg)
           (image/add-media-file! s)
           (db/create-media! s)
           (db/create-sighting! s)
           (db/create-photo! s)
           (db/create-sighting-field-values! s)))
    (assoc msg :result :complete
           :absolute-path (get-in msg [:record :absolute-path]))
    (catch Exception e
      (log/error (.getMessage e))
      (log/error (str/join "\n" (map str (.getStackTrace e))))
      (assoc msg :result :failed
             :absolute-path (get-in msg [:record :absolute-path])))))

(defn add-import-result
  "Update importer state with the latest result."
  [{:keys [state result absolute-path]}]
  (dosync
   (alter (get-in state [:importer :pending]) dec)
   (alter (get-in state [:importer result]) inc)
   (when (= result :failed)
     (alter (get-in state [:importer :failed-paths]) conj absolute-path))))

(defn media-processor
  "Setup a pipeline for parallel media import and process results."
  [num-importers]
  (let [ch (chan)
        result-chan (chan num-importers)]
    (async/pipeline-blocking num-importers result-chan (map do-import) ch)
    (go-loop []
      (let [msg (<! result-chan)]
        (add-import-result msg))
      (recur))
    ch))

(defn import-media-fn
  "Setup data structure and process media import."
  [num-importers]
  (let [proc (media-processor num-importers)]
    (fn [state record]
      (try
        (>!! proc (with-transaction [s state]
                    (let [r (->> record
                                 (db/get-survey s)
                                 (db/get-or-create-camera! s)
                                 (db/get-or-create-site! s)
                                 (db/get-or-create-survey-site! s)
                                 (db/get-or-create-trap-station! s)
                                 (db/get-or-create-trap-session! s)
                                 (db/get-or-create-trap-camera! s)
                                 (db/get-or-create-taxonomy! s)
                                 (db/get-or-create-survey-taxonomy! s)
                                 (hash-map :state state :record))]
                      r)))
        (catch Exception e
          (log/error (.getMessage e))
          (log/error (str/join "\n" (map str (.getStackTrace e))))
          (add-import-result {:state state
                              :result :failed
                              :absolute-path (:absolute-path record)}))))))

(defn file-data-to-record-list
  "Return a vector of maps, where each map contains all data for a record."
  [state file-data headings mappings]
  (map
   (fn [row]
     (reduce-kv (fn [acc k v]
                  (if (nil? v)
                    acc
                    (let [d (nth row (get headings v))]
                      (assoc acc k (datatype/deserialise-field k d))))) {} mappings))
   file-data))

(defn validate-and-import
  "Validate a seq of record, and if valid, queue for import."
  [state survey-id records]
  (let [problems (validate/validate state records)]
    (if (seq problems)
      (map :reason problems)
      (do
        (>!! (get-in state [:importer :cmd-chan])
             {:state state :cmd :new})
        (doseq [r (sort-by :media-capture-timestamp records)]
          (>!! (get-in state [:importer :queue-chan])
               {:state state
                :type :record
                :record r}))
        (>!! (get-in state [:importer :queue-chan])
             {:state state
              :type :finish
              :handler (delay (survey/set-bulk-import-mode! state survey-id false))})
        nil))))

(defn import-with-mappings
  "Given file data and a series of mappings, attempt to import it."
  [state {:keys [file-data mappings survey-id]}]
  (let [props (template/calculate-column-properties file-data)
        errs (model/check-mapping mappings props (partial tr/translate state))
        headings (reduce-kv #(assoc %1 %3 %2) {} (first file-data))]
    (if (seq errs)
      {:errors errs}
      (->> mappings
           (file-data-to-record-list state (rest file-data) headings)
           (map #(merge {:survey-id survey-id} %))
           (validate-and-import state survey-id)))))
