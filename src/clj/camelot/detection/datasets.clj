(ns camelot.detection.datasets
  (:require [camelot.util.state :as state]
            [clojure.core.async :as async]
            [clojure.walk :as walk]))

(defmacro context->state
  {:style/indent [2 [:defn]]}
  [{:keys [system-state ctx]} [state-binding] & body]
  `(let [id# (:dataset-id ~ctx)
         ~state-binding (state/with-dataset ~system-state id#)]
     ~@body))

(def async-put-fns
  #{#'async/put! #'async/>! #'async/offer! #'async/>!!})

(defn should-transform?
  [form]
  (when (and (list? form) (symbol? (first form)))
    (async-put-fns (ns-resolve *ns* (first form)))))

(defn build-transform-async-put-call
  [id]
  (fn [form]
    (let [[afn ch data] (into [] form)]
      (list afn ch `(assoc ~data :dataset-id ~id)))))

(defn create-form-transformer
  [pred f]
  (fn [form]
    (if (pred form)
      (f form)
      form)))

(defmacro state->context
  {:style/indent [1 [:defn]]}
  [state & body]
  `(do
     ~@(map #(let [tf (build-transform-async-put-call `(state/get-dataset-id ~state))]
               (walk/postwalk (create-form-transformer should-transform? tf) %))
            body)))

(defmacro with-context
  "Dataset context orchestration. Offers two capabilities:

  1. Produces `state` for a given context
  2. Rewrites all `core.async` `put!`-eque calls to assoc-in that context."
  [ks [state] & body]
  `(context->state ~ks [state#]
     (state->context state#
       (let [~state state#]
         ~@body))))
