(ns camelot.component.error
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [secretary.core :as secretary :refer-macros [defroute]]
            [camelot.nav :as nav]
            [camelot.translation.core :as tr]))

(def new-issue-url "http://bitbucket.org/cshclm/camelot/issues/new")

(defn error-dialog-content
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "content"}
               (dom/h3 #js {:className "error-title"}
                       (tr/translate ::problems))
               (dom/textarea #js {:className "error-text"
                                  :disabled "disabled"
                                  :rows 10
                                  :cols 55
                                  :value (:error data)})
               (dom/p #js {:className "error-paragraph"}
                      (tr/translate ::maybe-bug)
                      (dom/a #js {:href new-issue-url
                                  :target "_blank"
                                  :rel "noopener noreferrer"}
                             (tr/translate ::report-issue)))
               (dom/button #js {:className "error-ack btn btn-danger"
                                :onClick #(do (om/update! data :error nil)
                                              (nav/analytics-event "error"
                                                                   "acknowledge-button"))}
                           (tr/translate :words/acknowledge)))
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
               (dom/h2 nil (tr/translate ::page-not-found))))))
