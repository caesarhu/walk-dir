(ns walk-dir.main
  (:gen-class)
  (:require
    [duct.core :as duct]
    [walk-dir.system :refer [walk-logger]]))

(duct/load-hierarchy)

(defn -main [& args]
  (let [keys     (or (duct/parse-keys args) [:duct/daemon])
        profiles [:duct.profile/prod]]
    (-> (duct/resource "walk_dir/config.edn")
        (duct/read-config)
        (duct/exec-config profiles keys))))
