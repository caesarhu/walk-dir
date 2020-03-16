(ns walk-dir.kodi-poster
  (:require
    [clojure.string :as str]
    [datoteka.core :as fs]
    [clojure2d.core :as c2d]))

(defn nfo?
  [path]
  (let [file-ext (str/lower-case (fs/ext path))]
    (= "nfo" file-ext)))

(defn save-image-to
  [from to]
  (when (fs/exists? from)
    (c2d/save (c2d/load-image from) to)))

(def re-nfo #"(.*)(\.nfo)$")
(def kodi-poster-append "-poster.jpg")
(def kodi-fanart-append "-fanart.jpg")
(def re-kodi-poster #".+-poster.jpg$")
(def re-kodi-fanart #".+-fanart.jpg$")
(def re-plex-thumb #".*thumb.png")
(def plex-poster-name #"poster.jpg")
(def plex-thumb-name #"thumb.jpg")
(def plex-fanart-name #"fanart.jpg")

(defn copy-image-kodi
  [file]
  (when (nfo? file)
    (let [header (second (re-matches re-nfo (str file)))
          png-file (str header ".png")
          jpg-file (str header ".jpg")
          kodi-poster (str header kodi-poster-append)
          kodi-fanart (str header kodi-fanart-append)]
      (save-image-to png-file kodi-poster)
      (save-image-to jpg-file kodi-fanart)
      (println (str "Processed " file)))))

(defn copy-image-plex
  [file]
  (when (nfo? file)
    (let [header (second (re-matches re-nfo (str file)))
          png-file (str header ".png")
          jpg-file (str header ".jpg")
          parent (str (fs/parent file) "/")
          plex-poster (str parent "poster.jpg")
          plex-fanart (str parent "fanart.jpg")
          plex-thumb (str parent "thumb.jpg")]
      (save-image-to png-file plex-poster)
      (save-image-to jpg-file plex-fanart)
      (save-image-to jpg-file plex-thumb)
      (println (str "Processed " file)))))

(defn delete-kodi-image
  [file]
  (when (or (re-matches re-kodi-poster (str file))
            (re-matches re-kodi-fanart (str file))
            (re-matches re-plex-thumb (str file)))
    (fs/delete file)))

(defn delete-plex-image
  [file]
  (let [file-name (fs/name file)]
    (when (or (re-matches plex-poster-name file-name)
              (re-matches plex-thumb-name file-name)
              (re-matches plex-fanart-name file-name))
      (fs/delete file))))

(defn kodi-style
  [file]
  (delete-plex-image file)
  (copy-image-kodi file))

(defn to-kodi
  [files]
  (doall (map kodi-style files)))

(defn plex-style
  [file]
  (delete-kodi-image file)
  (copy-image-plex file))

(defn to-plex
  [files]
  (doall (map plex-style files)))