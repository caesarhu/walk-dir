(ns walk-dir.srt
  (:require
    [datoteka.core :as fs]
    [clojure.string :as str]
    [duct.logger :refer [log]]
    [walk-dir.system :refer [walk-logger]]
    [walk-dir.files-fn :refer [do-files-fn]]
    [clojure.java.shell :refer [sh]]))

(defn ext?
  [expect-ext path]
  (= (str/lower-case expect-ext) (str/lower-case (fs/ext path))))

(defn to-utf8
  [file]
  (let [tmp-file (fs/create-tempfile :suffix ".srt")]
    (try
      (let [res-1 (sh "ffmpeg" "-y" "-i" (str file) (str tmp-file))
            res-2 (if (= 0 (:exit res-1))
                    res-1
                    (sh "ffmpeg" "-y" "-sub_charenc" "gb18030" "-i" (str file) (str tmp-file)))]
        (if (= 0 (:exit res-2))
          (do
            (fs/move tmp-file file #{:replace})
            (log @walk-logger :info ::to-utf8-success {:file file})
            file)
          (do
            (when (fs/exists? tmp-file)
              (fs/delete tmp-file))
            (log @walk-logger :error ::to-utf8-error {:file file}))))
      (catch Exception ex
        (log @walk-logger :error ::to-utf8-error {:file file
                                                  :message (.getMessage ex)}))
      (finally
        (when (fs/exists? tmp-file)
          (fs/delete tmp-file))))))

(defn s2twp
  [file]
  (let [tmp-file (fs/create-tempfile :suffix ".srt")]
    (try
      (let [result (sh "opencc" "-i" (str file) "-o" (str tmp-file) "-c" "s2twp.json")
            successful? (= (:err result) "")]
        (if successful?
          (do
            (fs/move tmp-file file #{:replace})
            (log @walk-logger :info ::s2twp-success {:file file})
            file)
          (do
            (when (fs/exists? tmp-file)
              (fs/delete tmp-file))
            (log @walk-logger :error ::s2twp-error {:file file}))))
      (catch Exception ex
        (log @walk-logger :error ::s2twp-error {:file file
                                                :message (.getMessage ex)}))
      (finally
        (when (fs/exists? tmp-file)
          (fs/delete tmp-file))))))

(defn do-srt-file
  [srt-file]
  (when (to-utf8 srt-file)
    (s2twp srt-file)))

(defn filter-srt-files
  [files]
  (filter #(ext? "srt" %) files))

(defn do-srt-files
  [files]
  (do-files-fn files do-srt-file filter-srt-files))