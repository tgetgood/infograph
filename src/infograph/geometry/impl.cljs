(ns infograph.geometry.impl
  "Linear Algebra in Two Dimensions."
  (:refer-clojure :exclude [vector])
  (:require [infograph.geometry.protocols :as p]))

;; REVIEW: This is really verbose. Because the type of both operands counts, the
;; number of midifications goes up by the square of the number of types. This
;; won't scale. It also won't have to because there are fundamentally only three
;; types in 2 dimensional linear algebra. This technique won't do when it comes
;; to distances.

(declare matrix)
(declare vector)
(declare scalar)

(defn err [& args]
  (throw (js/Error. (apply str args))))

(defrecord Vector [v]
  p/Geometric
  (p/+ [_ u]
    (mapv + v (:v u)))
  (p/- [_ u]
    (mapv - v (:v u)))
  (p/* [_ _]
    (err "Can't multiply with a vector on the left."))

  p/IVector
  (dot [_ u]
    (if (instance? Vector u)
      (reduce + (map * v (:v u)))
      (err "Dot product is only defined on vectors. Got a " (type u))))
  (norm [this]
    (js/Math.sqrt (p/dot this this))))

(defn mmult [m n]
  (throw (js/Error. "Not implemented")))

(defrecord Matrix [v]
  p/Geometric
  (p/+ [_ m]
    (if (instance? Matrix m)
      (matrix (mapv (partial mapv +) v (:v m)))
      (err "Can't add " (type m) " to a matrix")))
  (p/- [_ m]
    (if (instance? Matrix m)
      (matrix (mapv (partial mapv -) v (:v m)))
      (err "Can't subtract a " (type m) " from a matrix")))
  (p/* [_ m]
    (cond
      (instance? Vector m) (vector (mapv #(reduce + (mapv * % (:v m))) v))
      (instance? Matrix m) (mmult v m)
      :else                (err "Can't multiply a matrix by a " (type m)))))

;; TODO: Scalars should just be numbers. This is getting to be too much type
;; juggling
(defrecord Scalar [v]
  p/Geometric
  (p/+ [_ s]
    (cond
      (number? s)          (scalar (+ v s))
      (instance? Scalar s) (scalar (+ v (:v s)))
      :else                (err "Can't add a " (type s) " to a scalar")))
  (p/- [_ s]
    (cond
      (number? s)          (scalar (- v s))
      (instance? Scalar s) (scalar (- v (:v s)))
      :else                (err "Can't subtract a " (type s) " from a scalar.")))
  (p/* [_ s]
    (cond
      (number? s) (scalar (* v s))
      (instance? Scalar s) (scalar (* v (:v s)))
      (instance? Vector s) (vector (mapv (partial * v) (:v s)))
      (instance? Matrix s) (apply matrix (map (partial * v) (flatten s)))
      :else (err "Can't multiply a scalar by a " (type s)))))


(defn matrix
  ([m]
   {:pre [(vector? m) (every? vector? m) (every? number? (flatten m))]}
   (Matrix. m))
  ([a b c d]
   (matrix [[a b] [c d]])))

(defn vector [v]
  (Vector. v))

(defn scalar [s]
  (Scalar. s))

;;;;; Units

(defrecord Unit []
  p/Geometric
  (p/* [this o]
    o))

(defrecord Zero []
  p/Geometric
  (p/+ [this o] o)
  (p/* [this o] this)
  (p/- [this o]
    (p/* (scalar (- 1)) o)))

(def unit (Unit.))
(def zero (Zero.))
