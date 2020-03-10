(ns walk-dir.walk
  (:require
    [datoteka.core :as fs]
    [duct.logger :refer [log]]
    [walk-dir.system :refer [walk-logger]]))

(defn walk
  ([raw-directory files-fn dirs-fn]
   (let [directory (fs/normalize raw-directory)]
     (log @walk-logger :info ::starting-directory directory)
     (try
       (when (fs/exists? directory)
         (let [raw-dir (vec (filter some? (fs/list-dir directory)))
               files (filter fs/regular-file? raw-dir)
               dirs (filter fs/directory? raw-dir)]
           (if dirs-fn
             (doall (map dirs-fn dirs))
             (doall (map #(walk % files-fn) dirs)))
           (when files-fn
             (files-fn files))))
       (catch Exception ex
         (log @walk-logger :error ::peocessing-directory-fail {:directory directory
                                                               :message (.getMessage ex)})))
     (log @walk-logger :info ::finished-directory directory)))
  ([directory files-fn]
   (walk directory files-fn nil)))
