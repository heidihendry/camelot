(ns camelot.view
  "Top-level application view definitions."
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [camelot.state :as state]
            [camelot.component.survey.core :as survey]
            [camelot.component.bulk-import.core :as bulk-import]
            [camelot.component.bulk-import.mapper :as bulk-import-mapper]
            [camelot.component.deployment.core :as deployment]
            [camelot.component.organisation :as organisation]
            [camelot.component.nav :as nav]
            [camelot.util.cursorise :as cursorise]
            [smithy.core :as smithy]
            [camelot.nav :as cnav]
            [smithy.util :as util]
            [camelot.util.misc :as misc]
            [camelot.rest :as rest]
            [camelot.component.notification :as cnotif]
            [camelot.component.library.core :as library]
            [secretary.core :as secretary :refer-macros [defroute]]
            [camelot.component.report.core :as report]
            [camelot.component.camera.core :as camera]
            [camelot.component.species.core :as species]
            [camelot.component.site.core :as site]))

(defn load-resource-children
  "Update the state of the children defined for the selected resource type."
  [vs]
  (let [screen (util/get-screen vs)
        res (get-in screen [:sidebar :resource])
        id (get-in vs [:screen :id])
        ep (if id
             (str (get res :endpoint) "/" id)
             (get res :endpoint))]
    (rest/get-resource ep
                       #(om/update! (get vs :selected-resource)
                                    :children
                                    (:body %)))))

(defn validate
  "Check required fields against the entered data"
  [vs]
  (let [entries (get (util/get-screen vs) :schema)
        reqd (filter (fn [[k v]] (get-in v [:schema :required])) entries)
        invalid (filter #(let [data (get-in vs [:buffer (first %) :value])]
                           (or (nil? data) (= data "")
                               (and (re-matches #".*-id$" (name (first %)))
                                    (= "-1" data))
                               (and (coll? data) (empty? data)))) reqd)]
    (if (zero? (count invalid))
      (do (om/update! (get vs :selected-resource) :show-validations false)
          true)
      (do (om/update! (get vs :selected-resource) :show-validations true)
          false))))

(def generators
  "Mapping of keys to functions to generate drop-down menus."
  {:camera-statuses {:vkey :camera-status-id
                     :desc :camera-status-description
                     :baseurl "/camera-statuses"}
   :survey-sites-available {:vkey :site-id
                            :desc :site-name
                            :baseurl "/survey-sites"}
   :taxonomy-available {:vkey :taxonomy-id
                        :desc :taxonomy-label
                        :baseurl "/taxonomy"}
   :species-mass {:vkey :species-mass-id
                  :desc :species-mass-label
                  :baseurl "/species-mass"}
   :trap-station-session-cameras-available {:vkey :camera-id
                                            :desc :camera-name
                                            :baseurl "/trap-station-session-cameras"}})

(defn create [success-key error-key vs resources key]
  "Create the item in the buffer and view it in readonly mode."
  (let [parent-id (util/get-parent-resource-id vs)
        basedata (deref (get vs :buffer))
        data (if parent-id
               (assoc basedata (util/get-screen-resource vs :parent-id-key)
                      {:value parent-id})
               basedata)]
    (when (validate vs)
      (cnav/analytics-event "create" (util/get-resource-type-name vs))
      (rest/post-resource (util/get-endpoint vs) {:data data}
                          #(do
                             (load-resource-children vs)
                             (om/update! (get vs :screen) :mode :readonly)
                             (om/update! (get vs :selected-resource) :details (:body %))
                             (om/update! vs :buffer (:body %))
                             (get-in vs [:events-ref success-key]))))))

(defn map-to-params
  [m]
  (clojure.string/join "&" (reduce-kv #(do (conj %1 (str (name %2) "="
                                                         (if (aget %3 "getTime")
                                                           (.getTime %3)
                                                           %3)))) [] m)))

(defn create-nav [success-key error-key vs resources key]
  "Create the item in the buffer and view it in readonly mode."
  (let [basedata (deref (get vs :buffer))]
    (when (validate vs)
      (cnav/analytics-event "create-nav" (util/get-resource-type-name vs))
      (.open js/window (misc/with-baseurl (str (util/get-endpoint vs) "?"
                                               (map-to-params (cursorise/decursorise basedata))))))))

(defn submit-update
  [success-key error-key vs resources key]
  "Submit the buffer state and return to readonly mode."
  (let [cb (get-in vs [:events-ref success-key])]
    (when (validate vs)
      (om/update! resources key (deref (get vs :buffer)))
      (nav/settings-hide!)
      (cnav/analytics-event "update" (util/get-resource-type-name vs))
      (rest/put-resource (util/get-url vs) {:data (deref (get vs :buffer))}
                         #(do
                            (when-not (util/settings-screen? vs)
                              (load-resource-children vs)
                              (om/update! (get vs :screen) :mode :readonly))
                            (when cb
                              (cb)))))))

(defn cancel-update
  [event-key vs resources key]
  "Revert the buffer state and return to readonly mode."
  (om/update! vs :buffer (deref (get resources key)))
  (om/update! (get vs :selected-resource) :show-validations false)
  (nav/settings-hide!)
  (cnav/analytics-event "cancel-update" (util/get-resource-type-name vs))
  (when-not (util/settings-screen? vs)
    (om/update! (get vs :screen) :mode :readonly))
  (let [cb (get-in vs [:events-ref event-key])]
    (when cb
      (cb))))

(defn delete [success-key error-key vs resources key]
  "Delete the resource with `key'."
  (let [cb (get-in vs [:events-ref success-key])]
    (cnav/analytics-event "delete" (util/get-resource-type-name vs))
    (rest/delete-resource (util/get-url vs) {}
                          #(do (om/update! resources key {})
                               (om/update! vs :buffer {})
                               (load-resource-children vs)
                               (om/update! (get vs :screen) :mode :create)
                               (when cb
                                 (cb))))))

(defn delete-media [success-key error-key vs resources key]
  "Delete the resource with `key'."
  (let [cb (get-in vs [:events-ref success-key])]
    (cnav/analytics-event "delete-media" (util/get-resource-type-name vs))
    (rest/delete-resource (str (util/get-url vs) "/media") {}
                          #(do (om/update! (get vs :screen) :mode :create)
                               (when cb
                                 (cb))))))

(defn build-generator
  "Drop-down menu generator for the given view-state."
  [vs]
  (fn [template gendata gen genargs]
    (let [to-dropdown #(hash-map :vkey (get % (:vkey template))
                                 :desc (get % (:desc template)))]
      (rest/get-resource (str (get template :baseurl)
                              "/"
                              (if (= (get-in vs [:screen :mode]) :create)
                                "available"
                                "alternatives")
                              "/"
                              (get genargs :id))
                         #(om/update! gendata gen
                                      (conj (map to-dropdown (:body %))
                                            {:vkey "" :desc ""}))))))

(def actions
  "Mapping of actions to the corresponding action function."
  {:survey-sites (fn [vs rid]
                   (cnav/breadnav! (str "/survey-sites/" rid)
                                  (util/get-breadcrumb-label vs)
                                  (get-in vs [:selected-resource :details])))
   :trap-stations (fn [vs rid]
                    (cnav/breadnav! (str "/trap-stations/" rid)
                                   (util/get-breadcrumb-label vs)
                                   (get-in vs [:selected-resource :details])))
   :trap-station-sessions (fn [vs rid]
                            (cnav/breadnav! (str "/trap-station-sessions/" rid)
                                           (util/get-breadcrumb-label vs)
                                           (get-in vs [:selected-resource :details])))
   :trap-station-session-cameras (fn [vs rid]
                                   (cnav/breadnav! (str "/trap-station-session-cameras/" rid)
                                                  (util/get-breadcrumb-label vs)
                                                  (get-in vs [:selected-resource :details])))
   :media (fn [vs rid]
            (cnav/breadnav! (str "/media/" rid)
                            (util/get-breadcrumb-label vs)
                            (get-in vs [:selected-resource :details])))
   :photos (fn [vs rid]
             (cnav/breadnav! (str "/photos/" rid)
                             (util/get-breadcrumb-label vs)
                             (get-in vs [:selected-resource :details])))
   :sightings (fn [vs rid]
                (cnav/breadnav! (str "/sightings/" rid)
                                (util/get-breadcrumb-label vs)
                                (get-in vs [:selected-resource :details])))
   :edit-mode (fn [vs rid] (om/update! (get vs :screen) :mode :update))
   :load-resource-children load-resource-children
   :load-resource (fn [vs id]
                    (let [cs (get-in vs [:selected-resource :children])
                          rkey (get-in (util/get-screen vs) [:resource :id])
                          resource (->> cs
                                        (filter #(= (int id) (get % rkey)))
                                        (first))]
                      (when resource
                        (rest/get-resource (:uri resource)
                                           (fn [resp]
                                             (om/update! (get vs :selected-resource) :details (:body resp))
                                             (om/update! vs :buffer (:body resp))
                                             (if (get-in vs [:screen :resource-id])
                                               (om/update! (get vs :screen) :resource-id nil)
                                               (om/update! (get vs :screen) :mode :readonly)))))))
   :delete-media (fn [vs rid]
                   (let [screen (util/get-screen vs)]
                     (when (js/confirm "Are you sure you wish to delete all media taken in the session by this camera?")
                       (delete-media (get-in screen [:states :delete :submit :success :event])
                                     (get-in screen [:states :delete :submit :error :event])
                                     vs
                                     (get vs :selected-resource) :details))))
   :delete (fn [vs rid] (let [screen (util/get-screen vs)]
                          (when (js/confirm "Are you sure you wish to delete this?")
                            (delete (get-in screen [:states :delete :submit :success :event])
                                    (get-in screen [:states :delete :submit :error :event])
                                    vs (get vs :selected-resource) :details))))})

(def events
  "Mapping of events to event functions."
  {:settings-save (fn [d]
                    (om/update! (state/resources-state) :settings
                                (deref (get-in (state/app-state-cursor)
                                               [:view :settings :buffer]))))
   :settings-cancel #(identity 1)
   :build-generator build-generator
   :analytics-event (fn [event action] (cnav/analytics-event event action))
   :sidebar-item-click (fn [vs id]
                         (cnav/analytics-event "sidebar-navigate"
                                               (util/get-resource-type-name vs))
                         ((:load-resource actions) vs id))
   :sidebar-create-click (fn [vs]
                           (cnav/analytics-event "sidebar-create"
                                                (util/get-resource-type-name vs))
                           (om/update! vs :buffer {})
                           (om/update! (get vs :screen) :mode :create))
   :breadcrumb-item-click (fn [token vs state]
                            (cnav/breadnav-consume! token)
                            (om/update! (get vs :screen) :mode :readonly)
                            (om/update! (get vs :selected-resource) :details state)
                            (om/update! vs :buffer state))
   :nav-history #(get (state/app-state-cursor) :nav-history)
   :cancel-update cancel-update
   :create create
   :create-nav create-nav
   :update submit-update
   :delete delete})

(defn navbar
  "Render the navbar"
  []
  (om/root nav/nav-component state/app-state
           {:target (js/document.getElementById "navigation")}))

(def notification-dialog
  "Render the notification dialog"
  (om/root cnotif/notification-dialog-component state/app-state
           {:target (js/document.getElementById "notification-dialog")}))

(defn generate-view
  "Render the main page content"
  [view & [{:keys [survey-id page-id report-key camera-id site-id taxonomy-id
                   restricted-mode]}]]
  (if survey-id
    (do
      (om/update! (state/app-state-cursor) :selected-survey nil)
      (om/update! (state/app-state-cursor) :page-id nil)
      (rest/get-x (str "/surveys/" survey-id)
                  #(do (om/update! (state/app-state-cursor) :selected-survey (:body %))
                       (om/update! (state/app-state-cursor) :page-id page-id)
                       (om/root view state/app-state
                                {:target (js/document.getElementById "page-content")}))))
    (om/root view state/app-state
             {:target (js/document.getElementById "page-content")
              :opts {:report-key report-key
                     :camera-id camera-id
                     :taxonomy-id taxonomy-id
                     :restricted-mode restricted-mode
                     :site-id site-id}})))

(defn settings-menu-view
  "Render the settings panel"
  []
  (om/update! (state/app-state-cursor) :events events)
  (om/update! (state/app-state-cursor) :actions actions)
  (om/update! (state/app-state-cursor) :generators generators)
  (when (:settings (:screens (state/app-state-cursor)))
    (let [f (smithy/build-view-component :settings)]
      (om/root f state/app-state
               {:target (js/document.getElementById "settings")}))))

(defn page-content-view
  [type mode {:keys [id resource-id]}]
  (when (and (not (nil? (:view (state/app-state-cursor))))
             (not (nil? (:resources (state/app-state-cursor)))))
    (om/update! (get (state/app-state-cursor) :view) :content
                {:screen {:type type :mode mode :id id :resource-id resource-id}
                 :buffer {}
                 :selected-resource {}
                 :generator-data {}})
    (let [f (smithy/build-view-component :content)]
      (om/root f state/app-state
               {:target (js/document.getElementById "page-content")}))))

(defroute "/surveys" [] (page-content-view :survey :create {}))
(defroute "/surveys/:mode/:rid" [mode rid] (page-content-view :survey (keyword mode)
                                                              {:resource-id rid}))
(defroute "/trap-station-session-cameras/:id" [id] (page-content-view :trap-station-session-camera :create
                                                                        {:id id}))
(defroute "/trap-station-session-cameras/:id/:mode/:resource-id" [id mode resource-id]
  (page-content-view :trap-station-session-camera (keyword mode) {:resource-id resource-id
                                                                  :id id}))
(defroute "/trap-station-sessions/:id" [id] (page-content-view :trap-station-session :create
                                                                 {:id id}))
(defroute "/media/:id" [id] (page-content-view :media :create
                                                 {:id id}))
(defroute "/photos/:id" [id] (page-content-view :photo :create {:id id}))
(defroute "/sightings/:id" [id] (page-content-view :sighting :create {:id id}))
(defroute "/trap-stations/:id" [id] (page-content-view :trap-station :create {:id id}))
(defroute "/survey-sites/:id" [id] (page-content-view :survey-site :create {:id id}))
(defroute "/sites" [] (page-content-view :site :create {}))
(defroute "/cameras" [] (page-content-view :camera :create {}))
(defroute "/taxonomy" [] (page-content-view :taxonomy :create {}))
(defroute "/library/restricted" [] (generate-view library/library-view-component {:restricted-mode true}))
(defroute "/library" [] (generate-view library/library-view-component))
(defroute "/:survey/library" [survey] (generate-view library/library-view-component
                                                     {:survey-id survey}))
(defroute "/:survey/bulk-import" [survey] (generate-view bulk-import/bulk-import-view
                                                         {:survey-id survey}))
(defroute "/:survey/bulk-import/mapper" [survey] (generate-view bulk-import-mapper/bulk-import-mapping-view
                                                                {:survey-id survey}))
(defroute "/organisation" [] (generate-view organisation/organisation-view))
(defroute "/:survey" [survey] (generate-view survey/survey-view-component
                                             {:survey-id survey}))
(defroute "/taxonomy/:taxonomy-id" [taxonomy-id]
  (generate-view species/update-view {:taxonomy-id taxonomy-id}))
(defroute "/camera/:camera-id" [camera-id]
  (generate-view camera/manage-view {:camera-id camera-id}))
(defroute "/site/:site-id" [site-id]
  (generate-view site/manage-view {:site-id site-id}))
(defroute "/report/:report-key" [report-key]
  (generate-view report/configure-report-view {:report-key report-key}))
(defroute "/:survey/taxonomy" [survey]
  (generate-view species/manage-view {:survey-id survey}))
(defroute "/:survey/details" [survey]
  (generate-view survey/edit-details-view {:survey-id survey}))
(defroute "/:survey/sighting-fields" [survey]
  (generate-view survey/sighting-fields-view {:survey-id survey}))
(defroute "/:survey/deployments/create" [survey]
  (generate-view deployment/create-view-component {:survey-id survey}))
(defroute "/:survey/deployments/:trap-station-id/edit" [survey trap-station-id]
  (generate-view deployment/edit-view-component {:survey-id survey
                                                 :page-id trap-station-id}))
(defroute "/:survey/deployments/:trap-station-id" [survey trap-station-id]
  (generate-view deployment/deployment-view-component {:survey-id survey
                                                       :page-id trap-station-id}))
(defroute "/survey/create" [] (generate-view survey/create-view-component))
(defroute "*" [] (generate-view cnotif/not-found-page-component))
