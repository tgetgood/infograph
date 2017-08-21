(ns infograph.shapes
  (:require [infograph.canvas :as canvas]
            [infograph.geometry :as geometry]
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

(defmulti draw* (fn [ctx shape] (classify shape)))

(defmethod draw* :default
  [ctx shape]
  ;; TODO: This is a question. "I don't know what to do." should be rephrased
  ;; everywhere as "What should I do?".
  (.error js/console (str "I don't know how to draw a " (classify shape))))

(defmethod draw* nil
  [_ _]
  (.error js/console "I can't draw something without a :type."))

(defmethod draw* :circle
  [ctx {:keys [c r style]}]
  (canvas/circle ctx style c r))

(defmethod draw* :line
  [ctx {:keys [style p q]}]
  (when-not (some nil? q)
    (canvas/line ctx style p q)))

(defmethod draw* :rectangle
  [ctx {:keys [style p w h]}]
  (when (and w h)
    (canvas/rectangle ctx style p (geometry/v+ p w h))))

(defmethod draw* :frame
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
    (draw* ctx shape)))

(defn draw!
  "Guard around draw*. Main purpose is to add useful error messages."
  ;; TODO: use a logger so that errors don't just pop up in the console in
  ;; production.
  [ctx shape]
  (if (nil? shape)
    (.error js/console "nil shape passed into draw!")
    (draw* ctx shape)))

;; TODO: Really we want something like draw => if should-draw? try-draw,
;; try-draw => if can-draw? draw! else comp draw transform.
;;
;; transform is going to end up being something like a non deterministic type
;; inference like thing because there will be multiple transformations and
;; multiple valid states. What would be the criteria to guarantee that it always
;; terminates?

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
