(ns camelot.import.scan-dir
  (:require
   [schema.core :as s]
   [camelot.util.file :as file]
   [clojure.string :as str]
   [camelot.util.state :as state])
  (:import
   (java.util.regex Pattern)))

(defn- relative-path?
  "False if path starts with a forward-slash or drive-letter. True otherwise."
  [dir]
  (nil? (re-find #"^(/|[A-Z]:)" dir)))

(defn- detect-separator
  [path]
  (cond
    (nil? path) (file/path-separator)
    (re-find #"^[A-Z]:(?:\\|$)" path) "\\"
    (and (re-find #"\\" path) (relative-path? path)) "\\"
    :else "/"))

(defn- resolve-absolute-server-directory
  "Resolve a client directory, unifying it with the configured server directory, if possible."
  [server-base-dir client-dir]
  (let [svr-sep (detect-separator server-base-dir)
        svr-path (str/split (or server-base-dir "")
                            (re-pattern (str "\\" svr-sep)))]
    (->> client-dir
         detect-separator
         (str "\\")
         re-pattern
         (str/split client-dir)
         (drop-while #(not= % (last svr-path)))
         rest
         (apply conj svr-path)
         (str/join svr-sep))))

(defn- resolve-relative-server-directory
  "Resolve a directory relative to the configured server directory, if any."
  [server-base-dir client-dir]
  (let [svr-sep (detect-separator server-base-dir)
        svr-path (str/split (or server-base-dir "")
                                       (re-pattern (str "\\" svr-sep)))]
    (->> client-dir
         detect-separator
         (str "\\")
         re-pattern
         (str/split client-dir)
         (apply conj svr-path)
         (str/join svr-sep))))

(defn- strategic-directory-resolver
  "Resolve a directory, either relative to the base-dir or absolutely."
  [server-base-dir client-dir]
  (let [f (if (and (relative-path? client-dir) (not (nil? server-base-dir)))
            resolve-relative-server-directory
            resolve-absolute-server-directory)]
    (f server-base-dir client-dir)))

(defn- ^String re-quote
  [^String s]
  (Pattern/quote ^String s))

(defn resolve-server-directory
  "Resolve the directory, defaulting to the root path should the client attempt to escape it."
  [server-base-dir client-dir]
  (if server-base-dir
    (let [f-can (file/canonical-path (file/->file (strategic-directory-resolver server-base-dir client-dir)))
          s-can (file/canonical-path (file/->file server-base-dir))]
      (if (re-find (re-pattern (str "^" (re-quote s-can))) f-can)
        f-can
        s-can))
    client-dir))

(defn resolve-directory
  "Resolve a corresponding server directory for a given 'client' directory."
  [state client-dir]
  {:pre [(not (nil? client-dir))]}
  (let [root (state/lookup-path state :root)
        res (resolve-server-directory root client-dir)]
    (cond
      (and (empty? res) (nil? root)) client-dir
      (empty? res) root
      :else res)))
