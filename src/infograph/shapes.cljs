(ns infograph.shapes
  (:require [infograph.shapes.constructors :as constructors]
            [infograph.shapes.impl :as impl]
            [infograph.protocols :as protocols]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; Normalisation
;;;;;
;;;;; This is a bit of a magic crux of the problem. How do we specify new ways
;;;;; of representing a line? How do we allow analogy and ambiguity in
;;;;; representations?
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; Constraints
;;;;;
;;;;; How do we enforce parallelograms, squares, etc.?
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; Shapes
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Needs to be a type because I want custom conj behaviour.
(deftype Composite [shapes]
  cljs.core/ISet
  (^clj -disjoin [_ o]
   (Composite. (disj shapes o)))

  cljs.core/ICollection
  (^clj -conj [_ o]
   (Composite. (conj shapes o)))
  
  ;; Need to implement ISeqable otherwise the REPL errors out trying to print
  ;; this guy.
  ISeqable
  (^clj-or-nil -seq [_]
   (seq shapes))

  Object
  (toString [_]
    (str "infograph.shapes.Composite - "{:shapes shapes}))

  protocols/Drawable
  (protocols/draw! [this window]
    (.log js/console shapes)
    (doseq [shape shapes]
      (protocols/draw! shape window)))

  protocols/Projectable
  (protocols/project [_ window]
    (Composite. (into #{} (map #(protocols/project % window) shapes))))

  impl/Instantiable
  (impl/instantiate [_ data]
    (Composite. (impl/instantiate shapes data))))

(defn empty-composite []
  (Composite. #{}))

;;;;; Dev cruft

(def test-shape
  {:start [0 0]
   :end [500 200]
   :type :line})

(defrecord CartesianPlane []
    )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;; External API to Shapes
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def construction-map
  {:line constructors/line-constructor
   :rectangle constructors/rectangle-constructor
   :circle constructors/circle-constructor})

(def instantiate impl/instantiate)

#_(def axis-style
    {:stroke-style "rgba(0,0,0,0.2)"})

#_(defrecord Window [ctx window content]
    protocols/ViewFrame
    (protocols/refresh [this]
      (canvas/clear ctx)
      ;; TODO: Render a grid so that the window is more obvious...
      (let [{[x y] :origin z :zoom w :width h :height} window]
        (when (on-screen? [0 y] window)
          (canvas/line ctx axis-style (pixels this [0 y]) (pixels this [0 (+ y h)])))
        (when (on-screen? [x 0] window)
          (canvas/line ctx axis-style (pixels this [x 0]) (pixels this [(+ x w) 0]))))
      (protocols/draw! content this)))
