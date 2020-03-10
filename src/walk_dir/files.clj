(ns walk-dir.files
  (:require
    [datoteka.core :as fs]
    [clojure.string :as str]
    [duct.logger :refer [log]]
    [walk-dir.system :refer [walk-logger]]
    [clojure.java.shell :refer [sh]]))

;  搭配walk處理目錄下的檔案：
;  files: walk傳遞過來的檔案參數-複數
;  file-fn: 處理單一檔案的function
;  filter-fn: 過濾files的函數，只留下需要處理的檔案

(defn do-files-fn
  ([files file-fn filter-fn]
   (let [filtered-files (if filter-fn
                          (filter-fn files)
                          files)]
     (doall (map file-fn filtered-files))))
  ([files file-fn]
   (do-files-fn files file-fn nil)))


(defn ext?
  [expect-ext path]
  (= (str/lower-case expect-ext) (str/lower-case (fs/ext path))))

(defn filter-ext
  [expect-ext files]
  (filter #(ext? expect-ext %) files))

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
        (log @walk-logger :info ::ass-to-srt-success {:file srt-file})
        srt-file)
      (do
        (when (fs/exists? tmp-file)
          (fs/delete tmp-file))
        (log @walk-logger :error ::ass-to-srt-error {:file file})))))

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
