(ns camelot.component.notification
  (:require [om.core :as om :include-macros true]
            [om.dom :as dom :include-macros true]
            [secretary.core :as secretary :refer-macros [defroute]]
            [camelot.nav :as nav]
            [camelot.translation.core :as tr]))

(def new-issue-url "https://tree.taiga.io/project/cshclm-camelot/issues")
(def forum-url "https://groups.google.com/forum/#!forum/camelot-project")

(defn error-dialog-content
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "content"}
               (dom/h3 #js {:className "error-title"}
                       (tr/translate ::problems))
               (dom/p #js {:className "error-summary"}
                      (tr/translate ::generic-error))
               (dom/textarea #js {:className "error-text"
                                  :disabled "disabled"
                                  :rows 12
                                  :cols 55
                                  :value (:error data)})
               (dom/p #js {:className "need-help"}
                      (tr/translate ::maybe-bug)
                      (dom/a #js {:href new-issue-url
                                  :target "_blank"
                                  :rel "noopener noreferrer"}
                             (tr/translate ::report-issue))
                      " " (tr/translate :words/or-lc) " "
                      (dom/a #js {:href forum-url
                                  :target "_blank"
                                  :rel "noopener noreferrer"}
                             (tr/translate ::ask-on-forum))
                      ".")
               (dom/button #js {:className "error-ack btn btn-danger"
                                :onClick #(do (om/update! data :error nil)
                                              (nav/analytics-event "error"
                                                                   "acknowledge-button"))}
                           (tr/translate :words/acknowledge))))))

(defn info-dialog-content
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div #js {:className "content"}
               (dom/h3 #js {:className "info-title"}
                       (tr/translate ::problems))
               (dom/textarea #js {:className "info-text"
                                  :disabled "disabled"
                                  :rows 12
                                  :cols 55
                                  :value (get-in data [:notification :info])})
               (dom/p nil)
               (dom/button #js {:className "btn btn-primary"
                                :onClick #(do (om/update! data [:notification :info] nil)
                                              (nav/analytics-event "info-dialog"
                                                                   "close-button"))}
                           (tr/translate :words/close))))))

(defn notification-dialog-component
  [app owner]
  (reify
    om/IRender
    (render [_]
      (let [data (:display app)]
        (if (:error data)
          (om/build error-dialog-content data)
          (if (get-in data [:notification :info])
            (om/build info-dialog-content data)
            (dom/span nil "")))))))

(defn not-found-page-component
  "Page not found"
  [data owner]
  (reify
    om/IRender
    (render [_]
      (dom/div nil
               (dom/h2 nil (tr/translate ::page-not-found))))))
