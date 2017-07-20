(ns infograph.canvas
  (:require [infograph.canvas.impl :as impl]))

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

;;;;; Window

(defn adjust-origin
  "Given an origin, a centre of zoom and a zoom scale, return the new
  origin."
  [[x y] [zx zy] delta-z]
  [(/ (- zx x) delta-z) (/ (- zy y) delta-z)])

(defn adjust-zoom [z delta]
  ;; REVIEW: This may not be ideal...
  (+ z delta))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; External API
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn context [elem]
  (let [ctx (.getContext elem "2d")]
    (impl/Canvas. elem ctx)))

(def clear impl/clear)
(def line impl/line)
(def circle impl/circle)
(def rectangle impl/rectangle)
