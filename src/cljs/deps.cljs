;; Bridging for typeahead which uses reagent. Reagent 0.8.1 expects cljsjs
;; React 16.4, and om depends on React 15.5.4. This is part of making everyone
;; happy.
{:foreign-libs [{:file     "react/react.js"
                 :provides ["react"]
                 :global-exports {react React}}
                {:file     "react-dom/react-dom.js"
                 :provides ["react-dom"]
                 :requires ["react"]
                 :global-exports {react-dom ReactDOM}}]
 :externs ["react/externs.js" "react-dom/externs.js"]}
