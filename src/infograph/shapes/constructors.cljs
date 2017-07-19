(ns infograph.shapes.constructors
  (:require [infograph.shapes.impl
             :refer [ComputationSchema SubSchema ValueSchema]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; DSL
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn cursor [x]
  (SubSchema. [:input] x))

(defn value [q]
  (ValueSchema. q))

(defn computation [f q]
  (ComputationSchema. f q))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; Geometry
;; TODO: Eventually collect this stuff.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn norm
  [[x1 y1] [x2 y2]]
  (let [x (- x2 x1)
        y (- y2 y1)]
    (js/Math.sqrt (+ (* x x) (* y y)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; GUI Constructors
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn line-constructor
  [p]
  {:type :line
   :p p
   :q (value [:strokes 0 :current])})

(defn rectangle-constructor
  [p]
  {:type :rectangle
   :p p
   :q (value [:strokes 0 :current])})

(defn circle-constructor
  [c]
  {:type :circle
   :p c
   :r (computation [:strokes 0 :current] #(norm c %))})

;;;;; Dev cruft

(def example-line-schema
  {:type :line
   :p (SubSchema. [0] {:x (ValueSchema. [:x])
                        :y (ValueSchema. [:y])})
   :q [100 5]})
