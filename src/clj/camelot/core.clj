(ns camelot.core
  "Camelot - Camera Trap management software for conservation research.
  Core initialisation."
  (:require
   [camelot.app.transit :as transit]
   [camelot.db.migrate :refer [migrate]]
   [camelot.app.routes :refer [app-routes]]
   [environ.core :refer [env]]
   [ring.adapter.jetty :refer [run-jetty]]
   [clojure.tools.nrepl.server :as nrepl]
   [ring.middleware.stacktrace :refer [wrap-stacktrace-log]]
   [ring.middleware.multipart-params :refer [wrap-multipart-params]]
   [ring.middleware.params :refer [wrap-params]]
   [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
   [ring.middleware.transit :refer [wrap-transit-response wrap-transit-params]]
   [ring.middleware.session :refer [wrap-session]]
   [ring.middleware.session.cookie :refer [cookie-store]]
   [ring.middleware.gzip :refer [wrap-gzip]]
   [ring.middleware.logger :refer [wrap-with-logger]]
   [camelot.app.version :as version]
   [clojure.java.shell :refer [sh]])
  (:import
   (java.net URI NetworkInterface InetAddress)
   (java.awt Desktop)
   (java.util Enumeration))
  (:gen-class))

(defonce nrepl-server (when (env :camelot-debugger)
                        (nrepl/start-server :port 7888)))

(defn meaningful-address
  [n]
  (let [as (.getInetAddresses ^NetworkInterface n)
        check (atom nil)]
    (while (and (nil? @check) (.hasMoreElements ^Enumeration as))
      (let [a (.nextElement ^Enumeration as)]
        (when (and (not (.isLinkLocalAddress ^InetAddress a))
                   (= (type a) java.net.Inet4Address))
          (reset! check a))))
    @check))

(defn get-network-addresses
  []
  (let [ns (NetworkInterface/getNetworkInterfaces)
        check (atom [])]
    (while (.hasMoreElements ^Enumeration ns)
      (let [e (.nextElement ^Enumeration ns)
            r (meaningful-address e)]
        (when r
          (swap! check #(conj % r)))))
    (map #(.getHostAddress ^InetAddress %) @check)))

(defn- start-browser
  [port]
  (let [addr (str "http://localhost:" port "/")
        uri (new URI addr)]
    (try
      (if (Desktop/isDesktopSupported)
        (.browse (Desktop/getDesktop) uri))
      (catch java.lang.UnsupportedOperationException e
        (sh "bash" "-c" (str "xdg-open " addr " &> /dev/null &"))))))

(def http-handler
  "Handler for HTTP requests"
  (-> app-routes
      wrap-params
      wrap-multipart-params
      (wrap-session {:store (cookie-store {:key "insecureinsecure"})})
      (wrap-transit-response {:encoding :json, :opts transit/transit-write-options})
      (wrap-transit-params {:opts transit/transit-read-options})
      wrap-stacktrace-log
      (wrap-defaults api-defaults)
      wrap-with-logger
      wrap-gzip))

(defn -main [& args]
  (let [port (Integer. (or (env :camelot-port) 5341))]
    (migrate)
    (println (format "Camelot %s started on port %d.\n" (version/get-version) port))
    (println "You might be able to connect to it from the following addresses:")
    (->> (get-network-addresses)
         (mapcat #(InetAddress/getAllByName %))
         (map #(.getCanonicalHostName ^InetAddress %))
         (map #(println (format "  - http://%s:%d/" % port)))
         (doall))
    (when (some #(= "--browser" %) args)
      (start-browser port))
    (run-jetty http-handler {:port port :join? false})))
