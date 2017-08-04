(ns infograph.locator
  (:require [infograph.geometry :refer [norm]]
            [infograph.shapes :as shapes]))

(defn classify [shape point]
  (shapes/classify shape))

(defmulti dist classify)

(defmethod dist :line
  [{[x1 x2] :p [y1 y2] :q :as shape} [x y]]
  (let [m (/ (- y2 y1) (- x2 x1))
        im (- (/ 1 m))]
    [m im])
  )

(defmethod dist :circle
  [{:keys [c r]} p]
  (let [d (norm p (shapes/value c))]
    (js/Math.abs (- (shapes/value r)  d))))

