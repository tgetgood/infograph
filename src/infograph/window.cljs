(ns infograph.window
  (:refer-clojure :exclude [contains?])
  (:require [infograph.window.core :as core]
            [infograph.window.protocols :as impl]))

(defn create [ctx window content]
  (core/Window. ctx window content))

(def refresh impl/refresh)
(def contains? impl/contains?)
(def coords impl/coords)
(def pixels impl/pixels)

(def Drawable impl/Drawable)
