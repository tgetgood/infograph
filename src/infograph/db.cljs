(ns infograph.db
  (:require [infograph.input :as input]
            [infograph.shapes :as shapes]))

(def default-db
  {:data input/nice-map
   :input {:strokes {:start nil :current nil :end nil}}
   :canvas {:shape shapes/empty-composite
            :input-mode :grab
            :window {:zoom 1
                     :bottom-left [0 0]}}})

