(ns infograph.geometry
  "Geometric Utilities.")

(defn dot
  "Returns the Euclidean inner product of p and q."
  [p q]
  {:pre [(every? #(and (number? %) (not (js/isNaN %))) (concat p q))]}
  (reduce + (map * p q)))

(defn norm
  "Returns the norm (length) of vector v in Euclidean space."
  [v]
  (when v
    (js/Math.sqrt (dot v v))))

;; REVIEW: It feels like implementing addition on vectors for this would be bad
;; form, but it would also be so clean looking...
(defn sub [[x1 y1] [x2 y2]]
  [(- x1 x2) (- y1 y2)])

(defn dist
  "Returns the distance between p and q in R^n"
  [p q]
  (norm (sub q p)))
