(ns camelot.macros.ui.validation
  "Macros to ease writing validated components.")

(defn- build-reducer
  [vc acc exp]
  (conj acc (list (first exp) 'camelot.validation.validated-component/wrapper (nth exp 2)
                  {:opts (assoc (or (nth exp 3) {})
                                :validation-chan vc
                                :component (second exp))})))

;; TODO this needs to walk body to find things to expand.
(defmacro with-validation
  "Wrap components with a validator. Calls to om/build take 3 new arguments:
  `data-key` korks in data (as passed to the Om's build) for the value being
  validated.  `validators` is a seq of validation fuctions.  Finally `params`
  is the parameters to pass on to the component to be rendered.

  This currently assumes that all components will be rendered via Om's
  `build`, and that all components to be validated will be placed as **top
  level sexps** within within `body`."
  [component form-opts & body]
  (let [vc# (:validation-chan form-opts)
        fo# (dissoc form-opts :validation-chan)]
    (concat (list component fo#)
            (reduce (partial build-reducer vc#) [] body))))
