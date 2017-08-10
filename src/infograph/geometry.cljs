(ns infograph.geometry
    "Geometric Utilities.")

;;;; Extending arithmetic operators to handle vectors.

(defn v+ [p q]
  (mapv + p q))

(defn v- [p q]
  (mapv - p q))

(defn v*
  "Returns scalar multiplication of a*v"
  [a v]
  (when (and (number? a) (vector? v))
    (mapv (partial * a) v)))

(defn dot
  "Returns the Euclidean inner product of p and q."
  [p q]
  (when(every? #(and (number? %) (not (js/isNaN %))) (concat p q))
    (reduce + (map * p q))))

(defn norm
  "Returns the norm (length) of vector v in Euclidean space."
  [v]
  (when v
    (js/Math.sqrt (dot v v))))

(defn dist
  "Returns the distance between p and q in R^n"
  [p q]
  (if (and p q)
    (norm (v- q p))
    js/Infinity))
