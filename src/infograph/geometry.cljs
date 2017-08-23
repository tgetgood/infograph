(ns infograph.geometry
  "Geometric Utilities."
  (:refer-clojure :exclude [+ - * val vector])
  (:require [infograph.geometry.impl :as impl]
            [infograph.geometry.protocols :as p]))


;;;;; Geometric operators
(defn +
  ([] impl/zero)
  ([x] x)
  ([x y] (p/+ x y))
  ([x y & more] (reduce + (p/+ x y) more)))

(defn -
  ([] impl/zero)
  ([x] (p/- x))
  ([x y] (p/- x y))
  ([x y & more] (reduce - (p/- x y) more)))

(defn *
  ([] impl/unit)
  ([x] x)
  ([x y] (p/* x y))
  ([x y & more] (reduce * (p/* x y) more)))

(def norm p/norm)
(def dot p/dot)

;;;;; Types

(def scalar impl/scalar)
(def vector impl/vector)
(def matrix impl/matrix)

;; FIXME: Disturbing convention.
(def val (partial :v))

;;;;; Affine Transformations

(def y-inversion
  "Affine tx to convert image to CG coords (origin in top left) for rendering."
  {:m (matrix [[1 0] [0 -1]]) :b (vector [0 0])})

(defn compose [{M :m b :b} {N :m a :b}]
  {:m (+ M N)
   :b (+ a b)})

(defn atx [{:keys [m b]} v]
  (+ (* m v) b))

;;;;; Distance

;; The distance between vectors isn't well defined. When you impose the
;; semantics of vectors being points minus the origin then the distance between
;; those points is well defined. The two operations are not the same thing.
(defn dist
  "Returns the distance between p and q in R^n"
  [p q]
  (.log js/console p q)
  (if (and p q)
    (norm (- (vector q) (vector p)))
    js/Infinity))
