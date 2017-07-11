(ns infograph.shapes
  (:require [infograph.uuid :as uuid]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; Schemata Types
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defprotocol Instantiable
  (instantiate [this data]))

(deftype ValueSchema [path]
  Object
  (toString [_]
    (str path "-->")))

(deftype ShapeSchema [path shape]
  Object
  (toString [_]
    (str path "-->" shape)))

(defn map-instantiate
  "push instantiate down into map vals"
  ;; REVIEW: This is such a common pattern. Would a higher level function make
  ;; this easy or just more obscure? On second thought it might need to be a
  ;; macro.
  [this data]
  (into {}
        (map (fn [[k v]]
               [k (instantiate v data)])
          this)))

(defn sequence-instantiate
  [this data]
  (into (empty this)
        (map #(instantiate % data) this)))

(extend-protocol Instantiable
  default
  (instantiate [this _] this)

  cljs.core/PersistentArrayMap
  (instantiate [this data]
    (map-instantiate this data))

  cljs.core/PersistentHashMap
  (instantiate [this data]
    (map-instantiate this data))

  cljs.core/List
  (instantiate [this data]
    (sequence-instantiate this data))

  cljs.core/PersistentVector
  (instantiate [this data]
    (sequence-instantiate this data))

  ;; TODO: Other collections. LazySeqs might be fun, but then again I don't need
  ;; lazyness in things that have to be rendered every frame so why waste time?

  ValueSchema
  (instantiate [this data]
    (get-in data (.-path this)))

  ShapeSchema
  (instantiate [this data]
    (instantiate (.-shape this) (get-in data (.-path this)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def empty-composite
  {:type :composite
   :shapes #{}})

(defn line [p q]
  {:type line
   :p p
   :q q})

;; TODO: Types
(defn assoc-shape [c s]
  (update c :shapes conj s))

(defn dissoc-shape [c s]
  (update c :shapes disj s))

(def test-shape
  {:start [0 0]
   :end [500 200]
   :type :line})

(def example-line-schema
  {:type :line
   :p (ShapeSchema. [0] {:x (ValueSchema. [:x])
                        :y (ValueSchema. [:y])})
   :q [100 5]})
