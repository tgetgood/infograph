(ns infograph.window.core
  (:require [infograph.canvas :as canvas]
            [infograph.window.protocols :as protocols]))

;;;;; Pixel Math

(defn- on-screen?
  "Returns true if the point [x y] is in the given window."
  [[x y] {[ox oy] :bottom-left  :keys [width height zoom]}]
  (and (<= ox x (+ ox (* zoom width)))
       (<= oy y (+ oy (* zoom height)))))

(defn- pixels*
  "Takes coordinates in the cartesian plane, and a window on the plane and
  returns the appropriate pixel coordinates."
  [[x y] {[ox oy] :bottom-left :keys [zoom]}]
  ;; Might have flipped the ratio
  [(/ (- x ox) zoom) (/ (- y oy) zoom)])

;;;;; Window object

(def axis-style
  {:stroke-style "rgba(0,0,0,0.2)"})

(def pixels protocols/pixels)

(defrecord Window [ctx window content]
  protocols/ViewFrame
  (protocols/refresh [this]
    (canvas/clear ctx)
    (let [{[x y] :bottom-left z :zoom w :width h :height} window]
      (when (on-screen? [0 y] window)
        (canvas/line ctx axis-style (pixels this [0 y]) (pixels this [0 (+ y h)])))
      (when (on-screen? [x 0] window)
        (canvas/line ctx axis-style (pixels this [x 0]) (pixels this [(+ x w) 0]))))
    (protocols/draw! content this))
  (protocols/coords [_ [x y]]
    (let [{z :zoom [ox oy] :bottom-left} window]
      [(+ ox (* z x)) (+ oy (* z y))]))
  (protocols/contains? [_ c]
    (on-screen? c window))
  (protocols/pixels [_ c]
    (let [h (:height window)
          [x y] (pixels* c window)]
      [x (- h y)])))

