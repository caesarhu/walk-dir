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
