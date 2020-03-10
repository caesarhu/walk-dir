(ns walk-dir.subtitle
  (:require
    [datoteka.core :as fs]
    [clojure.string :as str]
    [duct.logger :refer [log]]
    [walk-dir.system :refer [walk-logger]]
    [clojure.java.shell :refer [sh]]
    [walk-dir.files :as files]))

;; ffmpeg 函數，諸如字幕檔轉碼成utf-8及ass to srt，都用此函數
(defn ffmpeg-process
  [file tmp-file]
  (let [pre-result (sh "ffmpeg" "-y" "-i" file tmp-file)
        result (if (= 0 (:exit pre-result))
                 pre-result
                 (sh "ffmpeg" "-y" "-sub_charenc" "gb18030" "-i" file tmp-file))]
    (if (= 0 (:exit result))
      (let [srt-file (str (first (fs/split-ext file)) ".srt")]
        (fs/move tmp-file srt-file #{:replace})
        (log @walk-logger :info ::ffmpeg-process-success {:file srt-file})
        srt-file)
      (do
        (when (fs/exists? tmp-file)
          (fs/delete tmp-file))
        (log @walk-logger :error ::ffmpeg-process-error {:file file})))))

;; 調用shell/sh處理檔案的常用流程

(defn to-str
  [obj]
  (if (string? obj)
    obj
    (str obj)))

(defn sh-file
  ([file sh-fn tmp-file]
   (let [t-file (if tmp-file
                  tmp-file
                  (fs/create-tempfile))]
     (try
       (sh-fn (to-str file) (to-str t-file))
       (catch Exception ex
         (log @walk-logger :error ::sh-file-fn-error {:file file
                                                      :error-message (.getMessage ex)}))
       (finally
         (when (fs/exists? t-file)
           (fs/delete t-file))))))
  ([file sh-fn]
   (sh-file file sh-fn nil)))

;; 將字幕檔由簡體轉換成台灣正體，如為ass字幕檔，同時轉換成srt字幕檔

(defn s2tw
  [file tmp-file]
  (let [result (sh "opencc" "-i" (str file) "-o" (str tmp-file) "-c" "s2twp.json")]
    (if (= (:err result) "")
      (do
        (fs/move tmp-file file #{:replace})
        (log @walk-logger :info ::s2tw-success {:file file})
        file)
      (do
        (when (fs/exists? tmp-file)
          (fs/delete tmp-file))
        (log @walk-logger :error ::s2tw-error {:file file})))))

(defn do-subtitle-file
  [srt-file]
  (when-let [result (sh-file srt-file ffmpeg-process (fs/create-tempfile :suffix ".srt"))]
    (sh-file result s2tw (fs/create-tempfile :suffix ".srt"))))

(defn do-srt-files
  [files]
  (files/do-files-fn files do-subtitle-file (partial files/filter-ext "srt")))

(defn do-ass-files
  [files]
  (files/do-files-fn files do-subtitle-file (partial files/filter-ext "ass")))
