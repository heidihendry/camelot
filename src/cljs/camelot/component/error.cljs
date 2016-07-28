(ns camelot.component.error
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [secretary.core :as secretary :refer-macros [defroute]]
            [camelot.nav :as nav]))

(def new-issue-url "http://bitbucket.org/cshclm/camelot/issues/new")

(defn error-dialog-content
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "content"}
               (dom/h3 #js {:className "error-title"}
                       "There were some problems...")
               (dom/textarea #js {:className "error-text"
                                  :disabled "disabled"
                                  :rows 10
                                  :cols 55
                                  :value (:error data)})
               (dom/p #js {:className "error-paragraph"}
                      "If you think this is a bug, please "
                      (dom/a #js {:href new-issue-url
                                  :target "_blank"}
                             "Report an Issue"))
               (dom/button #js {:className "error-ack btn btn-danger"
                                :onClick #(do (om/update! data :error nil)
                                              (nav/analytics-event "error" "acknowledge-button"))}
                           "Acknowledge"))
      )))

(defn error-dialog-component
  [app owner]
  (reify
    om/IRender
    (render [_]
      (let [data (:display app)]
        (if (:error data)
          (om/build error-dialog-content data)
          (dom/span nil ""))))))

(defn not-found-page-component
  "Page not found"
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (dom/h2 nil "Page Not Found")))))
