(ns camelot.util.network
  (:import
   (java.net NetworkInterface InetAddress)
   (java.util Enumeration)))

(defn- meaningful-address
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

(defn canonicalise-addresses
  [port]
  (->> (get-network-addresses)
       (mapcat #(InetAddress/getAllByName %))
       (map #(.getCanonicalHostName ^InetAddress %))
       (map #(format "http://%s:%d/" % port))))

(defn print-network-addresses
  [port]
  (->> (canonicalise-addresses port)
       (map println)
       (doall)))
