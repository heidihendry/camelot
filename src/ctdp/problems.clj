(ns ctdp.problems)

(def severities
  {:okay -1
   :ignore 0
   :info 1
   :warn 2
   :error 3})

(defn highest-severity
  [most-sev this-sev]
  (if (> (get severities this-sev)
         (get severities most-sev))
    this-sev
    most-sev))

(defn problem-handler
  [state f dir severity problem]
  (let [klookup #(keyword (str "problems/" (name %)))
        tlookup #((:translate state) (klookup %))]
    (when (> (get severities severity) (get severities :ignore))
      (f (tlookup severity) dir (tlookup problem)))
    severity))

(defn process-problems
  [state probf dir problems]
  (let [handlers (:problems (:config state))
        handlerfn #(problem-handler state probf dir (get handlers %) %)]
    (reduce highest-severity :okay (map handlerfn problems))))
