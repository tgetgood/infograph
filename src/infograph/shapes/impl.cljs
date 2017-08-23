(ns infograph.shapes.impl
  (:require-macros [infograph.shapes.impl :refer [extend-recursive-tx]])
  (:require [infograph.window :as window]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; Extension Helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn seq-recur
  "Apply (apply f x args) to each x in this."
  [f this & args]
  (into (empty this)
        (map #(apply f % args)) this))

(defn map-recur
  "Push f down into vals of this."
  [f this & args]
  (into {}
        (map (fn [[k v]]
               [k (apply f v args)])
          this)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; Protocols
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defprotocol Instantiable
  "Instantiables are computations that know how to resolve themselves given data
  that doesn't exist yet. Like lenses, sort of."
  (instantiate [this data]))

(defprotocol Projectable
  "Projectables are shapes expressed in Cartesian coordinates that can be
  transformed to pixel coordinates via linear (affine?) projection."
  (project [this window]))

;;; Generic Implementations

(extend-recursive-tx Instantiable instantiate)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; Schemata Types
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;; Instantiables

(defrecord ValueSchema [query])
(defrecord SubSchema [query shape])
(defrecord ComputationSchema [query formula])

(extend-protocol Instantiable
  ValueSchema
  (instantiate [this data]
    (get-in data (.-query this)))

  SubSchema
  (instantiate [this data]
    (if-let [cursor (get-in data (.-query this))]
      (instantiate (.-shape this) cursor)
      this))

  ComputationSchema
  (instantiate [this input]
    ((.-formula this) (get-in input (.-query this)))))
