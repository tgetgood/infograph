(ns infograph.shapes.constructors
  (:require [infograph.shapes.impl
             :refer [ComputationSchema SubSchema ValueSchema Coordinate-2D Scalar]
             :as impl]))

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
;;;;; Geometry
;; TODO: Eventually collect this stuff.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn norm
  [[x1 y1] [x2 y2]]
  (when (and x1 x2 y1 y2)
    (let [x (- x2 x1)
          y (- y2 y1)]
      (js/Math.sqrt (+ (* x x) (* y y))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; GUI Constructors
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn line-constructor [p]
  {:type :line
   :style {}
   :p (point p)
   :q (c-point [:strokes 0 :current])})

(defn rectangle-constructor [p]
  #_(base/Rectangle. {} p (value [:strokes 0 :current])))

(defn circle-constructor [c]
  {:type :circle
   :style {}
   :c (point c)
   :r (computation [:strokes 0 :current] #(Scalar. (norm c %)))})

;;;;; Dev cruft

(def example-line-schema
  {:type :line
   :style {}
   :p (SubSchema. [0] {:x (ValueSchema. [:x])
                       :y (ValueSchema. [:y])})
   :q [100 5]})
