(ns infograph.shapes.constructors
  (:require [infograph.shapes.base :as base]
            [infograph.shapes.impl
             :refer [ComputationSchema SubSchema ValueSchema] :as impl]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; DSL
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn cursor [x]
  (SubSchema. [:input] x))

(defn value [q]
  (cursor (ValueSchema. q)))

(defn computation [q f]
  (cursor (ComputationSchema. q f)))

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
  (base/Line. {} p (value [:strokes 0 :current])))

(defn rectangle-constructor [p]
  (base/Rectangle. {} p (value [:strokes 0 :current])))

(defn circle-constructor [c]
  (base/Circle. {} c (computation [:strokes 0 :current] #(norm c %))))

;;;;; Dev cruft

(def example-line-schema
  (base/Line.
   {}
   (SubSchema. [0] {:x (ValueSchema. [:x])
                    :y (ValueSchema. [:y])})
   [100 5]))
