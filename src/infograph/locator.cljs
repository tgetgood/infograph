(ns infograph.locator
  (:require [infograph.geometry :as geometry]
            [infograph.shapes :as shapes]))

(defn classify [shape point]
  (:type shape))

;; TODO: Move this into geometry. Distance is a naturally polymorphic function
;; of any two geometric structures. In principle it's the min over all points a
;; in A, b in B of ||a - b||, but that's not computationally useful.
;;
;; We'd need a multimethod that finds the type of each argument and then
;; switches off. Is there a way to enforce symmetry in the arguments
;; systematically? Feels like it's going to be awkward... macro magic?
(defmulti dist classify)

(defmethod dist :line
  [{:keys [p q]} c]
  (let [pq (geometry/- q p)
        t* (- (/ (geometry/dot (geometry/- p c) pq)
                 (geometry/dot pq pq)))
        t (min 1 (max 0 t*))
        s (geometry/+ p (geometry/* t pq))]
    (geometry/dist s c)))

(defmethod dist :circle
  [{:keys [c r]} p]
  (let [d (geometry/dist p (:v c))]
    (js/Math.abs (- (:v r)  d))))

(defmethod dist :rectangle
  [{:keys [p w h]} c]
  (if (and w h)
    (let [q  (geometry/+ p w h)
          p' [(first p) (second q)]
          q' [(first q) (second p)]]
      ;; REVIEW: This is the naive algorithm, but it comes out clinky, doesn't it?
      ;; The most natural implementation geometrically should be the natural way
      ;; to write the code should it not?
      (apply min
             (map #(dist % c)
               (map (fn [[p q]]
                      {:type :line
                       :p p
                       :q q})
                 [[p p'] [p' q] [q q'] [q' p]]))))
    js/Infinity))
