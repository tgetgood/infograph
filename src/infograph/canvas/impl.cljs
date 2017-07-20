(ns infograph.canvas.impl
  "Stateless wrapper for HTML Canvas."
  (:require-macros [infograph.canvas.impl :refer [with-style with-stroke]]))

;;;;; Wrappers for global setters

;; FIXME: little more than a stub.
(defn- save-style [ctx]
  {:stroke-style (.-strokeStyle ctx)})

(defn- set-style! [ctx style]
  (set! (.-strokeStyle ctx) (:stroke-style style)))

;;;;; Canvas

(defprotocol ICanvas
  "Wrapper around HTML Canvas elements with a stateless API. All stateful canvas
  setters are replaced by the style map."
  ;; TODO: Presumably I should wrap the entire canvas API.
  (clear [this])
  (line [this style p q])
  (rectangle [this style p q]
    "Rectangle defined by bottom left and top right corners")
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

