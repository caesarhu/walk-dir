(ns walk-dir.system
  (:require
    [integrant.core :as ig]
    [taoensso.timbre.appenders.3rd-party.rolling :as rolling])
  (:import
    java.util.Locale
    java.util.TimeZone))

(def walk-logger (atom nil))

(defmethod ig/init-key :duct.logger.timbre/rolling [_ options]
  (-> (rolling/rolling-appender options)
      (merge (select-keys options [:min-level]))))

(defmethod ig/init-key :walk-dir.timestamp-opts [_ options]
  (let [{:keys [pattern locale timezone]} options]
    {:pattern pattern
     :locale (java.util.Locale. locale)
     :timezone (java.util.TimeZone/getTimeZone timezone)}))

(defmethod ig/init-key :walk-logger [_ logger]
  (reset! walk-logger logger)
  logger)