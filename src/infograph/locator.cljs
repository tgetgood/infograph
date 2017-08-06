(ns infograph.locator
  (:require [infograph.geometry :as geometry]
            [infograph.shapes :as shapes]))

(defn classify [shape point]
  (shapes/classify shape))

(defmulti dist classify)

(defmethod dist :line
  [{:keys [p q]} c]
  (let [p (shapes/value p)
        q (shapes/value q)
        pq (geometry/v- q p)
        t* (- (/ (geometry/dot (geometry/v- p c) pq)
                 (geometry/dot pq pq)))
        t (min 1 (max 0 t*))
        s (geometry/v+ p (geometry/v* t pq))]
    (geometry/dist s c)))

(defmethod dist :circle
  [{:keys [c r]} p]
  (let [d (geometry/dist p (shapes/value c))]
    (js/Math.abs (- (shapes/value r)  d))))

(defmethod dist :rectangle
  [_ _]
  3000)
