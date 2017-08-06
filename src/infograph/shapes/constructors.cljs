(ns infograph.shapes.constructors
  (:require [infograph.geometry :as geometry]
            [infograph.shapes.impl
             :refer
             [ComputationSchema Coordinate-2D Scalar SubSchema ValueSchema]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; DSL
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn cursor [x]
  (SubSchema. [:input] x))

(defn value [q]
  (cursor (ValueSchema. q)))

(defn computation [q f]
  (cursor (ComputationSchema. q f)))

(defn point [[x y]]
  (Coordinate-2D. x y))

(defn c-point [query]
  (computation query point))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; GUI Constructors
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn line-constructor [p]
  {:type :line
   :style {}
   :p (point p)
   :q (c-point [:strokes 0 :current])})

(defn rectangle-constructor [p]
  {:type :rectangle
   :style {}
   :p (point p)
   :q (c-point [:strokes 0 :current])})

(defn circle-constructor [c]
  {:type :circle
   :style {}
   :c (point c)
   :r (computation [:strokes 0 :current] #(Scalar. (geometry/dist c %)))})

;;;;; Dev cruft

(def example-line-schema
  {:type :line
   :style {}
   :p (SubSchema. [0] {:x (ValueSchema. [:x])
                       :y (ValueSchema. [:y])})
   :q [100 5]})
