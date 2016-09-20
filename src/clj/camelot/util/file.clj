(ns camelot.util.file
  (:import [org.apache.commons.lang3 SystemUtils])
  (:require [clojure.string :as str]
            [camelot.util.java-file :as jf]
            [clojure.java.io :as io]))

(defn- path-separator-re
  []
  (if SystemUtils/IS_OS_WINDOWS
    #"\\"
    #"/"))

(defn rel-path-components
  "Return the relative path to `file' as a list of strings, each string representing a component of the path."
  [state file]
  (let [rp (jf/canonical-path (io/file (get-in state [:config :root-path])))]
    (str/split (subs (jf/canonical-path file) (inc (count rp))) (path-separator-re))))
