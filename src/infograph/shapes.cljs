(ns infograph.shapes
  (:require [infograph.uuid :as uuid]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; Extension Helpers
;;;;;
;;;;; It would be a lot nicer if we had clojure's extend macro.
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
;;;;; Schemata Types
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defprotocol Instantiable
  (instantiate [this data]))

(deftype ValueSchema [query]
  Object
  (toString [_]
    (str query "-->")))

(deftype ShapeSchema [query shape]
  Object
  (toString [_]
    (str query "-->" shape)))

(extend-protocol Instantiable
  default
  (instantiate [this _] this)

  cljs.core/PersistentArrayMap
  (instantiate [this data]
    (map-recur instantiate this data))

  cljs.core/PersistentHashMap
  (instantiate [this data]
    (map-recur instantiate this data))

  cljs.core/List
  (instantiate [this data]
    (seq-recur instantiate this data))

  cljs.core/PersistentVector
  (instantiate [this data]
    (seq-recur instantiate this data))

  cljs.core/PersistentHashSet
  (instantiate [this data]
    (seq-recur instantiate this data))

  cljs.core/PersistentTreeSet
  (instantiate [this data]
    (seq-recur instantiate this data))

  ;; TODO: Other collections. LazySeqs might be fun, but then again I don't need
  ;; lazyness in things that have to be rendered every frame so why waste time?

  ValueSchema
  (instantiate [this data]
    (get-in data (.-query this)))

  ShapeSchema
  (instantiate [this data]
    (instantiate (.-shape this) (get-in data (.-query this)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; Reactive Types
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defprotocol Reactive
  (react [this input]))

(deftype ReactiveValue [query])
(deftype ReactiveComputation [query formula])

(extend-protocol Reactive
  default
  (react [this _] this)

  cljs.core/PersistentArrayMap
  (react [this input]
    (map-recur react this input))

  cljs.core/PersistentHashMap
  (react [this input]
    (map-recur react this input))

  cljs.core/List
  (react [this input]
    (seq-recur react this input))

  cljs.core/PersistentVector
  (react [this input]
    (seq-recur react this input))

  cljs.core/PersistentHashSet
  (react [this input]
    (seq-recur react this input))

  cljs.core/PersistentTreeSet
  (react [this input]
    (seq-recur react this input))
    
  ReactiveValue
  (react [this input]
    (get-in input (.-query this)))

  ReactiveComputation
  (react [this input]
    ((.-formula this) (get-in input (.-query this)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; Shapes
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn line-constructor
  [p]
  {:type :line
   :p p
   :q (ReactiveValue. [:strokes 0 :current])})

(defn rectangle-constructor
  [p]
  {:type :rectangle
   :p p
   :q (ReactiveValue. [:strokes 0 :current])})

(defn norm
  [[x1 y1] [x2 y2]]
  (let [x (- x2 x1)
        t (- y2 y1)]
    (js/Math.sqrt (* x x) (* y y))))

(defn circle-constructor
  [c]
  {:type :circle
   :p c
   :r (ReactiveComputation. [:strokes 0 :current] #(norm c %))})

(def construction-map
  {:line line-constructor
   :rectangle rectangle-constructor
   :circle circle-constructor})

(defn line [p q]
  {:type :line
   :p p
   :q q})

(def empty-composite
  {:type :composite
   :shapes #{}})

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
