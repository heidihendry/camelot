(ns camelot.report.query
  "Efficiently perform large queries."
  (:require
   [schema.core :as s]
   [medley.core :as medley]
   [camelot.system.state :refer [State]]
   [camelot.model.camera :as camera]
   [camelot.model.camera-status :as camera-status]
   [camelot.model.media :as media]
   [camelot.model.photo :as photo]
   [camelot.model.sighting :as sighting]
   [camelot.model.site :as site]
   [camelot.model.taxonomy :as taxonomy]
   [camelot.model.species-mass :as species-mass]
   [camelot.model.survey :as survey]
   [camelot.model.survey-site :as survey-site]
   [camelot.model.trap-station :as trap-station]
   [camelot.model.trap-station-session :as trap-station-session]
   [camelot.model.trap-station-session-camera :as trap-station-session-camera]))

(def query-fns
  {:media-id {:all media/get-all*
              :media-id media/get-with-ids}
   :sighting-id {:all sighting/get-all*
                 :media-id sighting/get-with-media-ids}
   :taxonomy-id {:all taxonomy/get-all}
   :photo-id {:all photo/get-all*
              :media-id photo/get-with-media-ids}
   :trap-station-session-camera-id {:all trap-station-session-camera/get-all*
                                    :media-id trap-station-session-camera/get-with-media-ids}
   :camera-id {:all camera/get-all}
   :trap-station-session-id {:all trap-station-session/get-all*}
   :trap-station-id {:all trap-station/get-all*}
   :survey-site-id {:all survey-site/get-all*}
   :species-mass-id {:all species-mass/get-all}
   :camera-status-id {:all camera-status/get-all}
   :site-id {:all site/get-all}
   :survey-id {:all survey/get-all}})

(def data-definitions
  {:media-id [[:trap-station-session-camera-id] [:sighting-id :photo-id]]
   :sighting-id [[:taxonomy-id :media-id] []]
   :taxonomy-id [[:species-mass-id] [:sighting-id]]
   :photo-id [[:media-id] []]
   :trap-station-session-camera-id [[:trap-station-session-id :camera-id] [:media-id]]
   :camera-id [[:camera-status-id] [:trap-station-session-camera-id]]
   :trap-station-session-id [[:trap-station-id] [:trap-station-session-camera-id]]
   :trap-station-id [[:survey-site-id] [:trap-station-session-id]]
   :survey-site-id [[:survey-id :site-id] []]
   :species-mass-id [[] [:taxonomy-id]]
   :camera-status-id [[] [:camera-id]]
   :site-id [[] [:survey-site-id :trap-station-id]]
   :survey-id [[] [:survey-site-id]]})

(defn known-dep?
  [acc dep]
  (some? (some #{(first dep)} (map first acc))))

(defn resolution-order
  [data by]
  (letfn [(order [acc deps rdeps]
                (if (empty? deps)
                  (if (empty? rdeps)
                    acc
                    (let [cd (get data (ffirst rdeps))]
                      (recur (conj acc (first rdeps))
                             (let [ndeps (filter #(not (known-dep? acc %))
                                                 (mapv #(vector % %) (first cd)))]
                               (if (seq ndeps)
                                 (concat deps ndeps)
                                 deps))
                             (let [nrdeps (filter #(not (known-dep? acc %))
                                                  (mapv #(vector % (ffirst rdeps)) (second cd)))]
                               (if (seq nrdeps)
                                 (concat (rest rdeps) nrdeps)
                                 (rest rdeps))))))
                  (let [cd (get data (ffirst deps))]
                    (recur (conj acc (first deps))
                           (let [ndeps (filter #(not (known-dep? acc %))
                                               (mapv #(vector % %) (first cd)))]
                             (if (seq ndeps)
                               (concat (rest deps) ndeps)
                               (rest deps)))
                           (let [nrdeps (filter #(not (known-dep? acc %))
                                                (mapv #(vector % (ffirst deps)) (second cd)))]
                             (if (seq nrdeps)
                               (concat rdeps nrdeps)
                               rdeps))))))]
    (medley/distinct-by first (order [] [[by by]] []))))

(defn build-records
  [rorder data]
  (reduce (fn [acc [tbl key]]
            (mapcat (fn [a] (let [rs (get-in data [tbl key (get a key)])]
                              (if (seq rs)
                                (map #(merge a %) rs)
                                (list a))))
                    acc))
          (flatten (vals (get-in data (first rorder))))
          (rest rorder)))

(defn- join-all
  ([state by media-ids]
   (let [rorder (resolution-order data-definitions by)
         data (reduce (fn [acc [t1 [t2s]]]
                        (let [media-q (get-in query-fns [t1 :media-id])
                              fallback-q (get-in query-fns [t1 :all])
                              qres (if (and media-ids media-q)
                                     (media-q state media-ids)
                                     (fallback-q state))]
                          (assoc acc t1
                                 (reduce (fn [iacc t2] (assoc iacc t2 (group-by t2 qres)))
                                         {}
                                         t2s))))
                      {} (group-by first rorder))]
     (build-records rorder data)))
  ([state by]
   (join-all state by nil)))

(s/defn get-by :- [{s/Keyword s/Any}]
  "Retrieve the data for the given report type."
  [state :- State
   by :- s/Keyword]
  (if (= by :none)
    []
    (join-all state (if (= by :all)
                      :media-id
                      (keyword (str (name by) "-id"))))))

(s/defn get-media :- [{s/Keyword s/Any}]
  "Retrieve the data for the given report type."
  [state :- State
   media-ids]
  (if (empty? media-ids)
    []
    (join-all state :media-id media-ids)))
