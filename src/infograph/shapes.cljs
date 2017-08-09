(ns infograph.shapes
  (:require [infograph.canvas :as canvas]
            [infograph.shapes.constructors :as constructors]
            [infograph.shapes.impl :as impl]))

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

(defn empty-frame []
  {:type :frame
   :shapes #{}})

(defn conj-shape [c s]
  (update c :shapes conj s))

(defn disj-shape [c s]
  (update c :shapes disj s))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; Drawing
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn classify [x] (:type x))

(defmulti draw! (fn [ctx shape] (classify shape)))

(defmethod draw! :circle
  [ctx {:keys [c r style]}]
  (canvas/circle ctx style c r))

(defmethod draw! :line
  [ctx {:keys [style p q]}]
  (when-not (some nil? q)
    (canvas/line ctx style p q)))

(defmethod draw! :rectangle
  [ctx {:keys [style p q]}]
  (when-not (some nil? q)
    (canvas/rectangle ctx style p q)))

(defmethod draw! :frame
  [ctx {:keys [shapes]}]
  (canvas/clear ctx)
  ;; TODO: Draw grid
  ;;
  ;; Problem is that it needs knowledge of the window as well as access to the
  ;; context. So the grid has to be part of the composite that gets fed through
  ;; the projection.
  ;;
  ;; It would also be neat if the grid could be an infinite number of infinitely
  ;; long lines, that just get rendered on demand (lazily) so that we don't need
  ;; to worry about calculating them explicitely.
  (doseq [shape shapes]
    (draw! ctx shape)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;; Data Relation Creation
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- cursor [x]
  (impl/SubSchema. [:data] x))

(defn connection [q] 
  (cursor (impl/ValueSchema. q)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;; External API to Shapes
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def construction-map
  {:line constructors/line-constructor
   :rectangle constructors/rectangle-constructor
   :circle constructors/circle-constructor})

(def instantiate impl/instantiate)
(def project impl/project)
(def value impl/value)
