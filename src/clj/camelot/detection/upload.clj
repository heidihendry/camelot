(ns camelot.detection.upload
  (:require
   [clojure.string :as cstr]
   [clj-http.client :as http]
   [camelot.model.media :as model.media]
   [camelot.detection.state :as state]
   [camelot.detection.util :as util]
   [clojure.core.async :as async]
   [clojure.java.io :as io]
   [clojure.tools.logging :as log]
   [diehard.core :as dh]))

(def ^:private socket-timeout (* 300 1000))
(def ^:private connection-timeout (* 5 1000))

(defn- slurp-bytes
  "Slurp the bytes from a slurpable thing"
  [stream]
  (with-open [out (java.io.ByteArrayOutputStream.)
              in stream]
    (io/copy in out)
    (.toByteArray out)))

(defn- container-sas-to-blob-sas
  [sas filename]
  (let [[container query] (cstr/split sas #"\?")]
    (format "%s/%s?%s" container filename query)))

(def ^:private retry-policy
  {:max-retries 2
   :backoff-ms [1000 4000 2.0]
   :jitter-factor 0.1})

(defn- upload-sas
  [state sas media]
  (try
    (if-let [stream (model.media/read-media-file state (:media-filename media) :original)]
      (let [filename (format "%s.%s" (:media-id media) (:media-format media))]
        {:result :success
         :value (dh/with-retry retry-policy
                  (http/put (container-sas-to-blob-sas sas filename)
                            {:socket-timeout socket-timeout
                             :connection-timeout connection-timeout
                             :headers {"x-ms-blob-content-disposition"
                                       (format "attachment; filename=\"%s""\"" filename)
                                       "x-ms-blob-type" "BlockBlob"}
                             :body (slurp-bytes stream)}))})
      (do (log/warn "Skipping upload of" (:media-id media) "as input file was not found.")
          {:result :skipped}))
    (catch Exception e
      (log/error e)
      {:result :error
       :value e})))

(defn run
  "Upload all media receeived on the returned channel."
  [state detector-state-ref [submit-ch submit-cmd-ch] event-ch]
  (let [cmd-ch (async/chan (async/dropping-buffer 100))
        ch (async/chan 1000)]
    (async/go-loop []
      (let [[v port] (async/alts! [cmd-ch ch] :priority true)]
        (condp = port
          cmd-ch
          (condp = (:cmd v)
            :stop
            (do
              (log/info "Detector upload stopped")
              (async/put! submit-cmd-ch v))

            :pause
            (do
              (async/put! submit-cmd-ch v)
              (util/pause cmd-ch #(async/put! submit-cmd-ch %)))

            (do
              (async/put! submit-cmd-ch v)
              (recur)))

          ch
          (condp = (:action v)
            :upload
            (do
              (log/info "Uploading media with id" (:subject-id v) "and scid" (:container-id v))
              (if (state/can-upload? @detector-state-ref (:subject-id v))
                (let [task (state/get-task-for-session-camera-id @detector-state-ref (:container-id v))
                      upload-v (upload-sas state (get-in task [:container :readwrite_sas]) (:payload v))]
                  (log/info "Upload result: " (:result upload-v))
                  (condp = (:result upload-v)
                    :success
                    (do
                      (log/info "Upload of" (:subject-id v) "complete.")
                      (state/record-media-upload! detector-state-ref (:container-id v) (:subject-id v) "completed")
                      (async/>! event-ch {:action :upload-succeeded
                                          :subject :media
                                          :subject-id (:subject-id v)}))

                    :skipped
                    (do
                      (state/record-media-upload! detector-state-ref (:container-id v) (:subject-id v) "skipped")
                      (async/>! event-ch {:action :upload-skipped
                                          :subject :media
                                          :subject-id (:subject-id v)}))

                    :error
                    (do
                      (state/record-media-upload! detector-state-ref (:container-id v) (:subject-id v) "failed")
                      (async/>! event-ch {:action :upload-failed
                                          :subject :media
                                          :subject-id (:subject-id v)})
                      (log/warn "media" (:subject-id v) "failed with exception:" (:value upload-v))
                      (log/warn "scheduling upload retry for" (:subject-id v))
                      (async/go (async/>! ch v)))))
                (do
                  (when (state/media-upload-failed? @detector-state-ref (:subject-id v))
                    (async/>! event-ch {:action :upload-retry-limit-reached
                                        :subject :media
                                        :subject-id (:subject-id v)}))
                  (log/info "Media longer eligible for upload." (:subject-id v))))
              (recur))

            :presubmit-check
            (do
              (async/>! submit-ch v)
              (log/info "Scheduled presubmit check")
              (recur))))))
    [ch cmd-ch]))
