(ns camelot.validation.validated-component
  "Validation layer for components."
  (:require [om.core :as om]
            [om.dom :as dom]
            [camelot.rest :as rest]
            [camelot.translation.core :as tr]
            [camelot.state :as state]
            [camelot.nav :as nav]
            [cljs.core.async :refer [<! chan >!]]
            [goog.date :as date]
            [camelot.util.data :as data]
            [clojure.string :as str])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn validator
  "Create a new validator."
  [f msg]
  {::predicate f
   ::msg msg})

(defn required
  []
  (validator (fn [v]
               (not (or (nil? v) (or (and (coll? v) (empty? v))
                                     (and (string? v) (empty? (str/trim v)))))))
             (tr/translate ::not-empty)))

(defn required-if
  [pred]
  (validator (fn [v]
               (if (pred)
                 (not (or (nil? v) (and (or (string? v)
                                            (coll? v))
                                        (empty? v))))
                 true))
             (tr/translate ::not-empty)))

(defn keyword-like
  []
  (validator #(and (string? %) (re-find #"^[-a-z0-9]+$" %))
             (tr/translate ::not-keyword)))

(defn max-length
  [n]
  (validator (fn [x] (< (count x) n))
             (tr/translate ::too-long n)))

(defn unique
  [others]
  (validator (fn [x] (not (contains? others x)))
             (tr/translate ::not-distinct)))

(defn- validated?
  [state]
  (every? identity (vals state)))

(defn component-validator
  "Takes a channel on to which form-wide validation results should be placed,
  and returns a channel to take validation results from individual
  components."
  [result-chan]
  (let [c (chan)
        state (atom nil)]
    (go
      (loop []
        (let [{:keys [key success command]} (<! c)
              pstate @state]
          (when (not= command :unmount)
            (if (nil? @state)
              (reset! state (hash-map key success))
              (swap! state #(assoc % key success)))
            (let [vpass (validated? @state)]
              (if (or (and (nil? pstate) (empty @state))
                      (not= (validated? pstate) vpass))
                (>! result-chan {:validated vpass})))
            (recur)))))
    c))

(defn- apply-validator
  [data acc k {:keys [::predicate ::msg]}]
  (when-not (predicate data)
    (reduced k)))

(defn wrapper
  "Return `component`, wrapped in a validator.

  Usually this is not invoked directly, but injected via
  `camelot.macros.ui.validation/with-validation`.

  `validators` is a list of validators which must pass. See `validator'.
  `validation-chan` is the channel on to which validation results should be
  placed.  any parameters, such as opts, needing to be passed to the component
  can be passed via `params`."
  [data owner
   {:keys [data-key component validators validation-chan params]}]
  (reify
    om/IInitState
    (init-state [_]
      {::validator-failed nil})
    om/IRenderState
    (render-state [_ state]
      (when (get-in data (if (keyword? data-key) [data-key] data-key))
        (om/set-state! owner ::show-messages true))
      (let [result (reduce-kv (fn [acc k v] (apply-validator (get data data-key)
                                                             acc k v)) nil
                              validators)]
        (om/set-state! owner ::validator-failed result)
        (go (>! validation-chan {:key data-key :success (nil? result)})))
      (let [show-warning (and (::show-messages state)
                              (get-in validators [(::validator-failed state) ::msg]))]
        (dom/div #js {:className "validated-component"}
                 (om/build component data params)
                 (when show-warning
                   (dom/div #js {:className (str "validation-warning ")}
                            (get-in validators [(::validator-failed state) ::msg]))))))))
