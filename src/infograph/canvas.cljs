(ns infograph.canvas
  "Wrapper around HTML Canvas elements with a stateless API. All stateful canvas
  setters are replaced by a style map. As compatible with manual canvas
  manipulation as manual canvas manipulation is with itself."
  (:require-macros [infograph.canvas :refer [with-style with-stroke]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; Canvas Manipulation
;;
;; Best way I could find to dynamically get the correct dimensions of the canvas
;; element was to put it in a div with 100% height and width and then query that
;; div at runtime.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn canvas-container []
  (.getElementById js/document "canvas-container"))

(defn canvas-container-dimensions []
  [(.-clientWidth (canvas-container))
   (.-clientHeight (canvas-container))])

(defn set-canvas-size! [canvas [width height]]
  (set! (.-width canvas) width)
  (set! (.-height canvas) height))

;; FIXME:
;; The following might be more appropriately placed in the infograph.events.dom
;; ns. infograph.window? We probably need a new set of concerns to deal with thss

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
;;;;; Canvas Wrapper
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;;;; Protocol

(defprotocol ICanvas
  ;; TODO: Presumably I should wrap the entire canvas API.
  (clear [this])
  (pixel [this style p])
  (line [this style p q])
  (rectangle [this style p q]
    "Rectangle defined by bottom left and top right corners")
  (circle [this style centre radius]))

;;;;; Styling

;; FIXME: little more than a stub.
(defn- save-style [ctx]
  {:stroke-style (.-strokeStyle ctx)})

(defn- set-style! [ctx style]
  (set! (.-strokeStyle ctx) (:stroke-style style)))

;;;;; Canvas

(deftype Canvas [elem ctx]
  ICanvas
  (clear [_]
    (let [width (.-clientWidth elem)
          height (.-clientHeight elem)]
      (.clearRect ctx 0 0 width height)))
  (pixel [_ style [x y]]
    (with-style ctx style
      (.moveTo ctx x y)
      (.fillRect ctx x y 1 1)))
  (line [_ style [x1 y1] [x2 y2]]
    (with-style ctx style
      (with-stroke ctx
        (.moveTo ctx x1 y1)
        (.lineTo ctx x2 y2))))
  (rectangle [_ style [x1 y1] [x2 y2]]
    (with-style ctx style
      (with-stroke ctx
        (.moveTo ctx x1 y1)
        (.rect ctx x1 y1 (- x2 x1) (- y2 y1)))))
  (circle [_ style [x y] r]
    (with-style ctx style
      (with-stroke ctx
        (.moveTo ctx (+ r x) y)
        (.arc ctx x y r 0 (* 2 js/Math.PI))))))

(defn context [elem]
  (let [ctx (.getContext elem "2d")]
      (Canvas. elem ctx)))
