(ns camelot.util.desktop
  "Utils to support running within Electron.")

(def process (when (aget js/window "process")
               (js* "process")))

(defn is-desktop-mode?
  []
  (boolean (some-> process (aget "versions") (aget "electron"))))

#_(defn- dialog-module
    []
    (-> (require-js "electron") (aget "remote") (aget "dialog")))

#_(defn show-save-dialog
    []
    (.showSaveDialog (dialog-module)))
