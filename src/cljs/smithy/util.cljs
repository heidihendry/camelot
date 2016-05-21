(ns smithy.util)

(defn remove-item!
  [val data edit-key owner]
  (om/transact! data edit-key
                (fn [_] (->>
                         edit-key
                         (get data)
                         (deref)
                         (remove #{val})
                         (into [])))))

(defn add-item!
  [val data edit-key owner]
  (when (not (empty? val))
    (om/transact! data edit-key
                  (fn [_]
                    (->> [edit-key :value]
                         (get-in data)
                         (#(conj % val))
                         (into #{})
                         (into [])
                         (hash-map :value))))))

(defn add-metadata-item!
  [val data edit-key owner]
  (when (not (empty? val))
    (om/transact! data edit-key (fn [_]
                                  (->> [edit-key :value]
                                       (get-in data)
                                       (deref)
                                       (#(conj % val))
                                       (into [])
                                       (hash-map :value))))))

(defn set-unvalidated-text! [e data edit-key owner]
  (om/transact! data edit-key (fn [_] (.. e -target -value))))

(defn set-number! [e data edit-key owner]
  (if (re-matches #"^-?[\.0-9]*$" (.. e -target -value))
    (om/transact! data edit-key (fn [_] (reader/read-string (.. e -target -value))))
    (set! (.. e -target -value) (get data edit-key))))

(defn set-percentage! [e data edit-key owner]
  (let [input (.. e -target -value)]
    (if (and (re-matches #"^[.0-9]*$" input)
             (<= (reader/read-string input) 1.0))
      (om/transact! data edit-key (fn [_] (reader/read-string input)))
      (set! (.. e -target -value) (get data edit-key)))))

(defn set-coerced-value!
  [k]
  (fn [e data edit-key owner]
    (let [input (.. e -target -value)
          f (cond (= (type k) cljs.core/Keyword) (fn [_] (keyword input))
                  (number? k) (fn [_] (reader/read-string input))
                  :else (fn [_] input))]
      (om/transact! data edit-key f))))

(defn analytics-event
  [component action]
  (let [ga (aget js/window "ga")]
    (when ga
      (ga "send" "event" component action))))

(defn analytics-pageview
  [page]
  (let [ga (aget js/window "ga")]
    (when ga
      (ga "set" "page" page)
      (ga "send" "pageview"))))

(defn get-screen
  "Return the screen corresponding to the given view-state."
  [vs]
  (get (get vs :screens-ref) (get-in vs [:screen :type])))

(defn get-screen-resource
  "Return the resource `k' of the screen of the given view-state."
  [vs k]
  (get-in (get-screen vs) [:resource k]))

(defn get-endpoint
  "Return the current resource's endpoint."
  [vs]
  (get-screen-resource vs :endpoint))

(defn get-screen-title
  "Return the title of the screen of the given view-state."
  [vs]
  (get-screen-resource vs :title))

(defn get-resource-type-name
  "Get the name for the current screen's resource type."
  [vs]
  (name (get-in (get-screen vs) [:resource :type])))

(defn get-resource-name
  [vs]
  (let [namekey (get-in (get-screen vs) [:sidebar :resource :label])]
    (get-in vs [:selected-resource :details namekey :value])))

(defn get-breadcrumb-label
  [vs]
  (str (get-screen-title vs) ": " (get-resource-name vs)))

(defn get-resource-id
  "Return the ID of the current resource for this view-state."
  [vs]
  (let [rid (get-screen-resource vs :id)]
    (if (nil? rid)
      nil
      (get-in vs [:selected-resource :details rid :value]))))

(defn get-parent-resource-id
  "Return the parent resource's endpoint URL for the current view-state."
  [vs]
  (get-in vs [:screen :id]))

(defn get-url
  "Return the endpoint URL for the current view-state."
  [vs]
  (let [rid (get-resource-id vs)
        base (get-endpoint vs)]
    (if (nil? rid)
      base
      (str base "/" rid))))

(defn settings-screen?
  "Predicate for whether this view-state is for the settings screen."
  [vs]
  (= (get-screen-resource vs :type) :settings))
