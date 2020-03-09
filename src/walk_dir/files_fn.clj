(ns walk-dir.files-fn
  (:require
    [datoteka.core :as fs]
    [clojure.string :as str]
    [duct.logger :refer [log]]
    [walk-dir.system :refer [walk-logger]]))

(defn do-files-fn
  ([files file-fn filter-fn]
   (let [filtered-files (if filter-fn
                          (filter-fn files)
                          files)]
     (doall (map file-fn filtered-files))))
  ([files file-fn]
   (do-files-fn files file-fn nil)))
