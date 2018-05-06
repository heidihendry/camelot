(ns camelot.import.core
  (:require
   [clojure.core.async :refer [>!!]]
   [schema.core :as s]
   [clj-time.core :as t]
   [camelot.translation.core :as tr]
   [camelot.import.capture :as capture]
   [camelot.spec.schema.state :refer [State]]
   [camelot.model.trap-station-session :as trap-station-session]))

(defn valid-session-date?
  "Predicate returning true if given date lies between session start and end dates. False otherwise.

24-hour tolerence on session end date applies."
  [sess
   date]
  (not (or (nil? date)
           (t/before? date (:trap-station-session-start-date sess))
           (t/after? date (t/plus (:trap-station-session-end-date sess)
                                  (t/days 1))))))

(s/defn import-capture!
  [state :- State
   session-camera-id :- s/Int
   {:keys [content-type :- s/Str
           tempfile :- s/Str
           size :- s/Int]}]
  (let [sess (trap-station-session/get-specific-by-trap-station-session-camera-id
              state session-camera-id)
        photo (capture/read-photo state tempfile)]
    (if (or (nil? photo) (not (valid-session-date? sess (:datetime photo))))
      {:error (tr/translate state ::timestamp-outside-range)}
      (capture/create-media-and-image! state content-type tempfile size session-camera-id photo))))

(defn importer-state
  "Return the state of the importer."
  [state]
  {:counts {:complete @(get-in state [:importer :complete])
            :failed @(get-in state [:importer :failed])
            :pending @(get-in state [:importer :pending])
            :ignored @(get-in state [:importer :ignored])
            :queued (count (.buf (get-in state [:importer :queue-chan])))}
   :start-time @(get-in state [:importer :start-time])
   :failed-paths @(get-in state [:importer :failed-paths])
   :end-time (or @(get-in state [:importer :end-time])
                 (t/now))})

(defn cancel-import
  "Cancel a running import."
  [state]
  (>!! (get-in state [:importer :cmd-chan])
       {:state state
        :cmd :cancel})
  nil)
