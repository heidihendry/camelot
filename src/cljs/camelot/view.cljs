(ns camelot.view
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [camelot.state :as state]
            [camelot.component.albums :as albums]
            [camelot.component.analysis :as analysis]
            [camelot.component.albums :as calb]
            [camelot.component.surveys :as surveys]
            [camelot.component.nav :as nav]
            [camelot.component.import-dialog :as import]
            [smithy.core :as smithy]
            [camelot.nav :as cnav]
            [smithy.util :as util]
            [camelot.rest :as rest]
            [camelot.component.error :as cerr]
            [camelot.component.footer :as cfoot]
            [secretary.core :as secretary :refer-macros [defroute]]))

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

(defn cancel-update [event-key vs resources key]
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
                   (cnav/breadnav! (str "/#/survey-sites/" rid)
                                  (util/get-breadcrumb-label vs)
                                  (get-in vs [:selected-resource :details])))
   :trap-stations (fn [vs rid]
                    (cnav/breadnav! (str "/#/trap-stations/" rid)
                                   (util/get-breadcrumb-label vs)
                                   (get-in vs [:selected-resource :details])))
   :trap-station-sessions (fn [vs rid]
                            (cnav/breadnav! (str "/#/trap-station-sessions/" rid)
                                           (util/get-breadcrumb-label vs)
                                           (get-in vs [:selected-resource :details])))
   :trap-station-session-cameras (fn [vs rid]
                                   (cnav/breadnav! (str "/#/trap-station-session-cameras/" rid)
                                                  (util/get-breadcrumb-label vs)
                                                  (get-in vs [:selected-resource :details])))
   :import-media (fn [vs rid] (js/alert "Not yet implemented."))
   :edit-mode (fn [vs rid] (om/update! (get vs :screen) :mode :update))
   :load-resource-children load-resource-children
   :delete (fn [vs rid] (let [screen (util/get-screen vs)]
                          (when (js/confirm "Are you sure you wish to delete this?")
                            (delete (get-in screen [:states :delete :submit :success :event])
                                    (get-in screen [:states :delete :submit :error :event])
                                    vs (get vs :selected-resource) :details))))})

(def events
  "Mapping of events to event functions."
  {:settings-save (fn [d] (albums/reload-albums))
   :settings-cancel #(identity 1)
   :build-generator build-generator
   :analytics-event (fn [event action] (cnav/analytics-event event action))
   :metadata-schema #(state/metadata-schema-state)
   :sidebar-item-click (fn [uri vs]
                         (cnav/analytics-event "sidebar-navigate"
                                              (util/get-resource-type-name vs))
                         (rest/get-resource uri
                                            (fn [resp]
                                              (om/update! (get vs :screen) :mode :readonly)
                                              (om/update! (get vs :selected-resource) :details (:body resp))
                                              (om/update! vs :buffer (:body resp)))))
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
   :update submit-update
   :delete delete})

(defn navbar
  "Render the navbar"
  []
  (om/root nav/nav-component state/app-state
           {:target (js/document.getElementById "navigation")}))

(def footer
  "Render the footer"
  (om/root cfoot/footer-component state/app-state
           {:target (js/document.getElementById "footer")}))

(def error-dialog
  "Render the error dialog"
  (om/root cerr/error-dialog-component state/app-state
           {:target (js/document.getElementById "error-dialog")}))

(def import-dialog
  "Render the import dialog"
  (om/root import/import-dialog-component state/app-state
           {:target (js/document.getElementById "import-dialog")}))

(defn generate-view
  "Render the main page content"
  [view]
  (om/root view state/app-state
           {:target (js/document.getElementById "page-content")}))

(defn settings-menu-view
  "Render the settings panel"
  []
  (let [f (smithy/build-view-component :settings)]
    (om/update! (state/app-state-cursor) :events events)
    (om/update! (state/app-state-cursor) :actions actions)
    (om/update! (state/app-state-cursor) :generators generators)
    (om/root f state/app-state
             {:target (js/document.getElementById "settings")})))

(defn page-content-view
  [type mode & [id]]
  (when (and (not (nil? (:view (state/app-state-cursor))))
             (not (nil? (:resources (state/app-state-cursor)))))
    (om/update! (get (state/app-state-cursor) :view) :content
                {:screen {:type type :mode mode :id id} :buffer {}
                 :selected-resource {}
                 :generator-data {}})
    (let [f (smithy/build-view-component :content)]
      (om/root f state/app-state
               {:target (js/document.getElementById "page-content")}))))

(defroute "/#/dashboard" [] (generate-view calb/album-view-component))
(defroute "/#/surveys" [] (page-content-view :survey :create))
(defroute "/#/trap-station-session-cameras/:id" [id] (page-content-view :trap-station-session-camera :create id))
(defroute "/#/trap-station-sessions/:id" [id] (page-content-view :trap-station-session :create id))
(defroute "/#/trap-stations/:id" [id] (page-content-view :trap-station :create id))
(defroute "/#/survey-sites/:id" [id] (page-content-view :survey-site :create id))
(defroute "/#/sites" [] (page-content-view :site :create))
(defroute "/#/cameras" [] (page-content-view :camera :create))
(defroute "/#/analysis" [] (generate-view analysis/analysis-view-component))
(defroute "*" [] (generate-view cerr/not-found-page-component))
