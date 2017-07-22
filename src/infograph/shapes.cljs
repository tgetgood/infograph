(ns infograph.shapes
  (:require [infograph.shapes.constructors :as constructors]
            [infograph.shapes.impl :as impl]
            [infograph.window.protocols :as wp]))

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
   (seq {:shapes shapes}))

  Object
  (toString [_]
    (str "infograph.shapes.Composite - "{:shapes shapes}))

  wp/Drawable
  (wp/draw! [this window]
    (doseq [shape shapes]
      ;; 
      (wp/draw! shape window)))

  impl/Instantiable
  (impl/instantiate [_ data]
    (Composite. (impl/instantiate shapes data))))

(defn empty-composite []
  (Composite. #{} :composite))

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
