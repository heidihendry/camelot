(ns camelot.init
  (:require
   [camelot.rest :as rest]
   [camelot.state :as state]
   [om.core :as om :include-macros true]))

(defn disable-loading-screen
  []
  (set! (.. (js/document.getElementById "loading") -style -cssText) "display: none")
  (set! (.. (js/document.getElementById "navigation") -style -cssText) "")
  (set! (.. (js/document.getElementById "app") -style -cssText) ""))

(defn init-screen-state
  [cb]
  (om/update! (state/app-state-cursor) :invalidate-full-page true)
  (rest/get-screens
   #(do (om/update! (state/app-state-cursor) :organisation {})
        (om/update! (state/app-state-cursor) :screens (:body %))
        (om/update! (state/app-state-cursor) :bulk-import {})
        (om/update! (state/app-state-cursor) :library {:search {}
                                                       :search-results {}})
        (rest/get-resource "/surveys"
                           (fn [r] (om/update! (state/app-state-cursor) :survey {:list (:body r)})))
        (om/update! (state/app-state-cursor) :language :en)
        (om/update! (state/app-state-cursor) :display {:error nil})
        (om/update! (state/app-state-cursor) :view
                    {:settings {:screen {:type :settings
                                         :mode :update}
                                :selected-resource {:details (get (state/resources-state) :settings)}}})
        (om/update! (get-in (state/app-state-cursor) [:view :settings])
                    :buffer (deref (get (state/resources-state)
                                        (get-in (state/app-state-cursor)
                                                [:view :settings :screen :type]))))
        (cb)
        (disable-loading-screen)
        (om/update! (state/app-state-cursor) :invalidate-full-page false))))
