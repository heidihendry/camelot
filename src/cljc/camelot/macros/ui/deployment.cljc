(ns camelot.macros.ui.deployment
  "Macros for building the deployment UI.")

(def ^:private read-only-field-symbol-pattern
  "Regexp matching the symbol of all build-read-only-field-like macros."
  #".*build-read-only-(calculated-)?field$")

(defmacro with-builders
  "Inject variables into build-read-only macros."
  [container [om-build component translator data] & body]
  (apply conj (map #(if (re-find read-only-field-symbol-pattern (name (first %)))
                      (cons (first %) (conj (rest %)
                                            data translator component om-build))
                      %)
                   body)
         (reverse container)))

(defmacro build-read-only-field
  "Create a read-only field."
  [om-build component translator data field label-key]
  `(when (get-in ~data [:data ~field :value])
     (~om-build ~component ~data
               {:init-state {:field ~field
                             :label (~translator ~label-key)}})))

(defmacro build-read-only-calculated-field
  "Create a read-only field~ with the value formatted as a date."
  [om-build component translator data date-field label-key field-function]
  `(if-let [fv# (get-in ~data [:data ~date-field :value])]
     (~om-build ~component ~data
               {:init-state {:value (~field-function fv#)
                             :label (~translator ~label-key)}})))
