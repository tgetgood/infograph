(ns infograph.canvas
  (:require-macros [infograph.canvas :refer [with-style with-stroke]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Canvas Manipulation
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn canvas-container []
  (.getElementById js/document "canvas-container"))

(defn canvas-container-dimensions []
  [(.-clientWidth (canvas-container))
   (.-clientHeight (canvas-container))])

(defn set-canvas-size! [canvas [width height]]
  (set! (.-width canvas) width)
  (set! (.-height canvas) height))

;; The following might be more appropriately placed in the infograph.events.dom
;; ns.

(defn drag-location [e]
  [(.-pageX e) (.-pageY e)])

(defn click-location [e]
  ;;HACK: We really need to pass in the canvas...
  (let [c (js/document.getElementById "the-canvas")]
    [(- (.-pageX e) (.-offsetLeft c))
     (- (.-pageY e) (.-offsetTop c))]))

(defn touch-location [e]
  (when-let [t (aget (.-touches e) 0)]
    (click-location t)))

(defn analyse-zoom [ev]
  [(click-location ev) (.-deltaY ev)])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; Cartesian Plane
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;;;; Canvas

(defprotocol ICanvas
  "Wrapper around HTML Canvas elements with a stateless API. All stateful canvas
  setters are replaced by the style map."
  (clear [this])
  (line [this style p q])
  (circle [this style centre radius]))

(deftype Canvas [elem ctx]
  ICanvas
  (clear [_]
    (let [width (.-clientWidth elem)
          height (.-clientHeight elem)]
      (.clearRect ctx 0 0 width height)))
  (line [_ style [x1 y1] [x2 y2]]
    (with-style ctx style
      (with-stroke ctx
        (.moveTo ctx x1 y1)
        (.lineTo ctx x2 y2))))
  (circle [_ style [x y] r]
    (with-style ctx style
      (with-stroke ctx
        (.moveTo ctx (+ r x) y)
        (.arc ctx x y r 0 (* 2 js/Math.PI))))))

(defn context [elem]
  (let [ctx (.getContext elem "2d")]
    (Canvas. elem ctx)))

;;;;; Window

(defprotocol Drawable
  "High level interface to the canvas window. Main app should only ever interact
  with it at this level."
  (draw! [this content]
    "Clear the element and draw content in window."))

(defprotocol Erasable
  "Only top level canvas elements should be erasable, lest we start to promote
  non-immediate mode graphics."
  (clear! [this]))

(defn on-screen?
  "Returns true if the point [x y] is in the given window."
  [[x y] {[ox oy] :bottom-left  :keys [width height zoom]}]
  (and (< ox x (+ ox (* zoom width)))
       (< oy y (+ oy (* zoom height)))))

(defn pixels
  "Takes coordinates in the cartesian plane, and a window on the plane and
  returns the appropriate pixel coordinates."
  [[x y] {[ox oy] :bottom-left :keys [zoom]}]
  ;; Might have flipped the ratio
  [(/ (- x ox) zoom) (/ (fkahf\ - y oy) zoom)])

(defn adjust-origin
  "Given an origin, a centre of zoom and a zoom scale, return the new
  origin."
  [[x y] [zx zy] delta-z]
  [(/ (- zx x) delta-z) (/ (- zy y) delta-z)])

(defn adjust-zoom [z delta]
  ;; REVIEW: This may not be ideal...
  (+ z delta))

(declare draw)

(deftype Window [ctx window]
  Drawable
  (draw! [this shape]
    (draw this shape))
  
  Erasable
  (clear! [_]
    (clear ctx)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; Drawing
;; TODO: Use the specs to validate shapes. There's too much adhoc coordination
;; as it stands.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmulti draw (fn [window vo]  (:type vo)))

(defmethod draw :default [_ _ _] nil)

(defmethod draw :composite
  [window vo]
  (doseq [sub-vo (:shapes vo)]
    (draw! window sub-vo)))

(defmethod draw :line
  [window {:keys [p q]}]
  (line (.-ctx window) {} p q))

(defmethod draw :circle
  [window {:keys [p r]}]
  (circle (.-ctx window) {} p r))

#_(defmethod draw :rectangle
  [ctx {[x1 y1] :p [x2 y2] :q}]
  (.moveTo ctx x1 y1)
  (.rect ctx x1 y1 (- x2 x1) (- y2 y1)))

