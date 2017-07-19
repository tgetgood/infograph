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
  (let [c (js/document.getElementById "the-canvas")
        h (.-clientHeight c)]
    [(- (.-pageX e) (.-offsetLeft c))
     (- h (- (.-pageY e) (.-offsetTop c)))]))

(defn touch-location [e]
  (when-let [t (aget (.-touches e) 0)]
    (click-location t)))

(defn analyse-zoom [ev]
  [(click-location ev) (.-deltaY ev)])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; Cartesian Plane
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; FIXME: little more than a stub.
(defn- save-style [ctx]
  {:stroke-style (.-strokeStyle ctx)})

(defn- set-style! [ctx style]
  (set! (.-strokeStyle ctx) (:stroke-style style)))

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
    (let [h (.-clientHeight elem)]
      (with-style ctx style
        (with-stroke ctx
          (.moveTo ctx x1 (- h y1))
          (.lineTo ctx x2 (- h y2))))))
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
  (draw! [this window] "Draw this in the given window")
  (draw? [this window] "Is this visible in the given window?"))

(defprotocol ViewFrame
  (refresh [this]
    "Clear and reset the window for a new drawing."))

(defn on-screen?
  "Returns true if the point [x y] is in the given window."
  [[x y] {[ox oy] :bottom-left  :keys [width height zoom]}]
  (and (<= ox x (+ ox (* zoom width)))
       (<= oy y (+ oy (* zoom height)))))

(defn visible?
  "Returns true if a shape is large enough inthe window to be worth rendering."
  ;;TODO: How do we decide? Right now if it's smaller than a pixel then we
  ;;render it as a single pixel,
  [& args]
  ;; FIXME: stub
  true)

(defn pixels
  "Takes coordinates in the cartesian plane, and a window on the plane and
  returns the appropriate pixel coordinates."
  [[x y] {[ox oy] :bottom-left :keys [zoom]}]
  ;; Might have flipped the ratio
  [(/ (- x ox) zoom) (/ (- y oy) zoom)])

(defn adjust-origin
  "Given an origin, a centre of zoom and a zoom scale, return the new
  origin."
  [[x y] [zx zy] delta-z]
  [(/ (- zx x) delta-z) (/ (- zy y) delta-z)])

(defn adjust-zoom [z delta]
  ;; REVIEW: This may not be ideal...
  (.log js/console delta)
  (+ z delta))

(def axis-style
  {:stroke-style "rgba(255, 0, 0, 0.2)"})

(defrecord Window [ctx window content]
  ViewFrame
  (refresh [this]
    (clear ctx)
    (let [{[x y] :bottom-left z :zoom w :width h :height} window]
      (line ctx {} [0 0] [w h])
      (when (on-screen? [0 y] window)
        (line ctx axis-style (pixels [0 y] window) (pixels [0 (+ y h)] window)))
      (when (on-screen? [x 0] window)
        (line ctx axis-style (pixels [x 0] window) (pixels [(+ x w) 0] window))))
    (draw! content this)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; Drawing
;; TODO: Use the specs to validate shapes. There's too much adhoc coordination
;; as it stands.
;;
;; This should very likely be pushed up into types for the individual shapes.
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
