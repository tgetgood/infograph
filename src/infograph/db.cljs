(ns infograph.db
  (:require [infograph.input :as input]
            [infograph.shapes :as shapes]))

(def default-db
  {:data input/nice-map
   :canvas-input-mode :none
   :canvas shapes/empty-composite})

