(ns dev
  (:refer-clojure :exclude [test])
  (:require [clojure.repl :refer :all]
            [fipp.edn :refer [pprint]]
            [clojure.tools.namespace.repl :refer [refresh]]
            [clojure.java.io :as io]
            [duct.core :as duct]
            [duct.core.repl :as duct-repl]
            [eftest.runner :as eftest]
            [integrant.core :as ig]
            [integrant.repl :refer [clear halt go init prep reset]]
            [integrant.repl.state :refer [config system]]
            [clojure.string :as str]
            [datoteka.core :as fs]
            [walk-dir.system :refer [walk-logger]]
            [walk-dir.walk :refer [walk]]
            [walk-dir.files :as files]
            [clojure.java.shell :refer [sh]]
            [walk-dir.subtitle :as sub]
            [java-time :as jt]
            [java-time.repl :as jr]
            [walk-dir.kodi-poster :as kodi]))

(duct/load-hierarchy)

(defn read-config []
  (duct/read-config (io/resource "walk_dir/config.edn")))

(defn test []
  (eftest/run-tests (eftest/find-tests "test")))

(def profiles
  [:duct.profile/dev :duct.profile/local])

(clojure.tools.namespace.repl/set-refresh-dirs "dev/src" "src" "test")

(when (io/resource "local.clj")
  (load "local"))

(integrant.repl/set-prep! #(duct/prep-config (read-config) profiles))
