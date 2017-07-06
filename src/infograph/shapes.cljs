(ns infograph.shapes)

;;TODO: Look up computed observables
(defprotocol IDynamicValue)

(def test-shape
  {:start [0 0]
   :end [500 200]
   :type :line})

(def data-bound-shape
  (fn [d]
    {:start [0 0]
     :end [(:x d) (:y d)]
     :type :line}))

(defn instantiate [shape data]
  (if (fn? shape)
    (shape data)
    shape))
