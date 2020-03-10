(ns walk-dir.srt
  (:require
    [datoteka.core :as fs]
    [clojure.string :as str]
    [duct.logger :refer [log]]
    [walk-dir.system :refer [walk-logger]]
    [walk-dir.files :as files]
    [clojure.java.shell :refer [sh]]))

;; 將srt字幕檔由簡體轉換成台灣正體

(defn s2tw
  [file tmp-file]
  (let [result (sh "opencc" "-i" (str file) "-o" (str tmp-file) "-c" "s2twp.json")]
    (if (= (:err result) "")
      (do
        (fs/move tmp-file file #{:replace})
        (log @walk-logger :info ::ass-to-srt-success {:file file})
        file)
      (do
        (when (fs/exists? tmp-file)
          (fs/delete tmp-file))
        (log @walk-logger :error ::ass-to-srt-error {:file file})))))

(defn do-srt-file
  [srt-file]
  (when (files/sh-file srt-file files/ffmpeg-process (fs/create-tempfile :suffix ".srt"))
    (files/sh-file srt-file s2tw (fs/create-tempfile :suffix ".srt"))))

(defn do-srt-files
  [files]
  (files/do-files-fn files do-srt-file (partial files/filter-ext "srt")))