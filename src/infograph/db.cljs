(ns infograph.db
  (:require [infograph.input :as input]
            [infograph.shapes :as shapes]))

;; TODO: spec the app-db. But not until it's relatively stable
(def default-db
  {:data input/nice-map
   :input {:strokes {:start nil :current nil :end nil}
           :drag-position [0 0]}
   :canvas {:shape shapes/empty-composite
            :input-mode :grab
            :window {:zoom 1
                     :bottom-left [0 0]
                     :width 0
                     :height 0}}})

