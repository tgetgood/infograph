(ns infograph.shapes.constructors
  (:require [infograph.geometry :as geometry]
            [infograph.shapes.impl :as impl]
            [infograph.geometry.impl :refer [Vector Scalar]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; DSL
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn cursor [x]
  (impl/SubSchema. [:input] x))

(defn value [q]
  (cursor (impl/ValueSchema. q)))

(defn computation [q f]
  (cursor (impl/ComputationSchema. q f)))

(defn point [v]
  (Vector. v))

(defn c-point [query]
  (computation query point))

(defn v2 [x y]
  (Vector. [x y]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; GUI Constructors
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn line-constructor [p]
  {:type  :line
   :style {}
   :p     (point p)
   :q     (c-point [:strokes 0 :current])})

(defn rectangle-constructor [p]
  {:type  :rectangle
   :style {}
   :p     (point p)
   :w     (computation [:strokes 0 :current]
                       #(when % (v2 (- (first %) (first p)) 0)))
   :h     (computation [:strokes 0 :current]
                       #(when % (v2 0 (- (second %) (second p)))))})

(defn circle-constructor [c]
  {:type  :circle
   :style {}
   :c     (point c)
   :r     (computation [:strokes 0 :current]
                       #(Scalar. (geometry/dist c %)))})
