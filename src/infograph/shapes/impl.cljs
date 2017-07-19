(ns infograph.shapes.impl)

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
;;;;; Schemata Types
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defrecord ValueSchema [query])
(defrecord SubSchema [query shape])
(defrecord ComputationSchema [query formula])

(defprotocol Instantiable
  "Instantiables are computations that know how to resolve themselves given data
  that doesn't exist yet. Like lenses, sort of."
  (instantiate [this data]))

(extend-protocol Instantiable
  default
  (instantiate [this _] this)

  cljs.core/PersistentArrayMap
  (instantiate [this data]
    (map-recur instantiate this data))

  cljs.core/PersistentHashMap
  (instantiate [this data]
    (map-recur instantiate this data))

  cljs.core/PersistentTreeMap
  (instantiate [this data]
    (map-recur instantiate this data))

  cljs.core/PersistentTreeMapSeq
  (instantiate [this data]
    (seq-recur instantiate this data))

  cljs.core/List
  (instantiate [this data]
    (seq-recur instantiate this data))

  cljs.core/PersistentVector
  (instantiate [this data]
    (seq-recur instantiate this data))

  cljs.core/PersistentQueue
  (instantiate [this data]
    (seq-recur instantiate this data))
  
  cljs.core/PersistentHashSet
  (instantiate [this data]
    (seq-recur instantiate this data))

  cljs.core/PersistentTreeSet
  (instantiate [this data]
    (seq-recur instantiate this data))

  ;; TODO: Rest of the collection types
  
  ValueSchema
  (instantiate [this data]
    (get-in data (.-query this)))

  SubSchema
  (instantiate [this data]
    (instantiate (.-shape this) (get-in data (.-query this))))

  ComputationSchema
  (instantiate [this input]
    ((.-formula this) (get-in input (.-query this))))) 
