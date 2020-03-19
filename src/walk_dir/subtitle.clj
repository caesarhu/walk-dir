(ns walk-dir.subtitle
  (:require
    [datoteka.core :as fs]
    [clojure.string :as str]
    [duct.logger :refer [log]]
    [walk-dir.system :refer [walk-logger]]
    [clojure.java.shell :refer [sh]]
    [walk-dir.files :as files]
    [java-time :as jt]))

(defn change-ext
  [file ext]
  (str (first (fs/split-ext file)) ext))

(defn detect-encode
  [file]
  (let [result (sh "enca" file)
        out (:out result)
        encode (str/upper-case (last (re-find #";\s+(.+)\n" out)))]
    (case encode
      "GB2312" "GB18030"
      "BIG5" "BIG5"
      nil)))

;; ffmpeg 函數，諸如字幕檔轉碼成utf-8及ass to srt，都用此函數
;; 以ffmpeg轉檔，可以避免enca轉碼提前中斷未轉換完全的錯誤
(defn ffmpeg-process
  [file tmp-file]
  (let [encode (detect-encode file)
        result (if encode
                 (sh "ffmpeg" "-y" "-sub_charenc" encode "-i" file tmp-file)
                 (sh "ffmpeg" "-y" "-i" file tmp-file))]
    (if (= 0 (:exit result))
      (let [srt-file (change-ext file ".srt")]
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
       (when (fs/exists? file)
         (sh-fn (to-str file) (to-str t-file)))
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
  (let [result (sh "opencc" "-i" file "-o" tmp-file "-c" "s2twp.json")]
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

(def re-tag #"<[^>]*>")

(defn remove-tag-str
  [tag-str]
  (str/replace tag-str re-tag ""))

(defn remove-tag-file
  [file]
  (when (fs/exists? file)
    (spit file (remove-tag-str (slurp file)))))

(def re-style-pre #"\{.*\}")
(def re-style #"^\s*m\s.*$")

(defn remove-style-str
  [in-str]
  (let [style-lines (str/split-lines in-str)
        remove-pre-lines (map #(str/replace % re-style-pre "") style-lines)
        removed-lines (map #(str/replace % re-style "") remove-pre-lines)]
    (str/join "\n" removed-lines)))

(defn remove-style-file
  [file]
  (when (fs/exists? file)
    (spit file (remove-style-str (slurp file)))))

(defn do-srt-files
  [files]
  (files/do-files-fn files do-subtitle-file (partial files/filter-ext "srt")))

(defn do-ass-files
  [files]
  (files/do-files-fn files do-subtitle-file (partial files/filter-ext "ass"))
  (files/do-files-fn (map #(change-ext % ".srt") files) remove-tag-file (partial files/filter-ext "srt"))
  (files/do-files-fn (map #(change-ext % ".srt") files) remove-style-file (partial files/filter-ext "srt")))

(def re-srt-time #"\d\d:\d\d:\d\d,\d\d\d")
(def re-adjust-time #"([+|-])(\d\d:\d\d:\d\d.\d\d\d)")
(def srt-time-format "HH:mm:ss,SSS")
(def base-time (jt/local-time "00:00:00"))

(defn parse-srt-time
  [srt-time]
  (jt/local-time srt-time-format srt-time))

(defn to-srt-time
  [t]
  (jt/format srt-time-format t))

(defn duration
  [adjust-str]
  (when-let [[_ direction time] (re-matches re-adjust-time adjust-str)]
    (let [dt (jt/duration base-time (jt/local-time time))]
      (if (= "-" direction)
        (jt/negate dt)
        dt))))

(defn adjust-srt-time-str
  [srt-time-str duration-time]
  (let [origin-time (parse-srt-time srt-time-str)]
    (to-srt-time (jt/plus origin-time duration-time))))

(defn adjust-srt-time
  [file duration-str]
  (let [duration-time (duration duration-str)
        file-str (slurp file)
        adjusted-str (str/replace file-str re-srt-time #(adjust-srt-time-str % duration-time))]
    (spit file adjusted-str)))

(defn adjust-time-files
  [files duration-str]
  (files/do-files-fn files #(adjust-srt-time % duration-str) (partial files/filter-ext "srt")))
