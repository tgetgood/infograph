(ns infograph.locator
  (:require [infograph.geometry :as geometry]
            [infograph.shapes :as shapes]))

(defn classify [shape point]
  (shapes/classify shape))

;; TODO: Move this into geometry. Distance is a naturally polymorphic function
;; of any two geometric structures. In principle it's the min over all points a
;; in A, b in B, but that's not computationally useful.
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
