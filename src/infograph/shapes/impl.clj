(ns infograph.shapes.impl)

(def map-types
  '[cljs.core/PersistentArrayMap
    cljs.core/PersistentHashMap])

(def seq-like-types
  '[cljs.core/PersistentTreeMap
    cljs.core/PersistentTreeMapSeq
    cljs.core/List
    cljs.core/PersistentVector
    cljs.core/PersistentQueue
    cljs.core/PersistentHashSet
    cljs.core/PersistentTreeSet])

(defn method-body [method recur]
  `(~method [this# val#]
    (~recur ~method this# val#)))

(defmacro extend-recursive-tx
  "Boilerplate removal. Assumes method only takes one arg (plus this)."
  [protocol method]
  `(extend-protocol ~protocol
     ~'default
     (~method [this# _#] this#)

     ~@(interleave map-types
         (repeat (method-body method 'infograph.shapes.impl/map-recur)))

     ~@(interleave seq-like-types
         (repeat (method-body method 'infograph.shapes.impl/seq-recur)))

     ;; TODO: LazySeq
     ))
