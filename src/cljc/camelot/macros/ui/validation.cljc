(ns camelot.macros.ui.validation
  "Macros to ease writing validated components."
  (:require
   #?(:clj [riddley.walk :as walk])))

(defn- validated-field?
  "Predicate returning true if sexp looks a field needing validation. False
  otherwise."
  [sexp]
  (and (seq? sexp)
       (= (count sexp) 4)
       (or (= (name (first sexp)) "build")
           (= (name (first sexp)) "build-all"))
       (every? #(contains? (nth sexp 3) %) [:data-key :validators])))

(defn- wrap-component
  "Wraps the sexp with a validation component."
  [vc sexp]
  (list (first sexp) 'camelot.validation.validated-component/wrapper (nth sexp 2)
        {:opts (assoc (or (nth sexp 3) {})
                      :validation-chan vc
                      :component (second sexp))}))

(defmacro with-validation
  "Wrap components with a validator. Calls to om/build take 3 new arguments:
  `data-key` korks in data (as passed to the Om's build) for the value being
  validated.  `validators` is a seq of validation fuctions.  Finally `params`
  is an optional map of parameters to pass to the component."
  [component form-opts & body]
  (let [vc# (:validation-chan form-opts)]
    (list* component (list 'clj->js (dissoc form-opts :validation-chan))
           (walk/walk-exprs validated-field? (partial wrap-component vc#)
                            body))))
