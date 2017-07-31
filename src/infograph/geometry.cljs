(ns infograph.geometry
  "Geometric Utilities.")

(defn norm
  "Returns the Euclidean norm of two points represented as vectors.
  N.B.: Currently only implemented in 2 dimensions"
  [p q]
  {:pre [(every? #(and (number? %) (not (js/isNaN %))) (concat p q))]}
  (when (and p q)
    (js/Math.sqrt
     (reduce (fn [acc [a b]]
               (let [d (- a b)]
                 (+ acc (* d d))))
             0
             (map vector p q)))))
