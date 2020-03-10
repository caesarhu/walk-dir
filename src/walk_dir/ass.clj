(ns walk-dir.ass
  (:require
    [datoteka.core :as fs]
    [clojure.string :as str]
    [duct.logger :refer [log]]
    [walk-dir.walk :refer [walk]]
    [walk-dir.system :refer [walk-logger]]
    [walk-dir.files :as files]
    [walk-dir.srt :as srt]))

;; 將ass字幕檔轉換成srt，並將簡體轉換成台灣正體

(defn ass->srt
  [file]
  (files/sh-file file files/ffmpeg-process (fs/create-tempfile :suffix ".srt")))

(defn do-ass-file
  [ass-file]
  (when-let [ass-result (ass->srt ass-file)]
    (srt/do-srt-file ass-result)))

(defn transfer-ass-files
  [files]
  (files/do-files-fn files do-ass-file (partial files/filter-ext "ass")))
