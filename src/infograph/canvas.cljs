(ns infograph.canvas)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Canvas Manipulation
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn canvas []
  (.getElementById js/document "the-canvas"))

(defn canvas-container []
  (.getElementById js/document "canvas-container"))

(defn get-ctx []
  (when-let [c (canvas)]
    (.getContext c "2d")))

(defn canvas-container-dimensions []
  [(.-clientWidth (canvas-container))
   (.-clientHeight (canvas-container))])

(defn set-canvas-size! [canvas [width height]]
  (set! (.-width canvas) width)
  (set! (.-height canvas) height))

(defn clear! [ctx]
  (let [[width height] (canvas-container-dimensions)]
    (.clearRect ctx 0 0 width height)))

;; The following might be more appropriately placed in the infograph.events.dom
;; ns.

(defn drag-location [e]
  [(.-pageX e) (.-pageY e)])

(defn click-location [e]
  (let [c (canvas)]
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

(defn on-screen?
  "Returns true if the point [x y] is in the given window."
  [[x y] {[ox oy] :bottom-left  :keys [width height zoom]}]
  (and (< ox x (+ ox (* zoom width))) (< oy y (+ oy (* zoom height)))))

(defn pixels
  "Takes coordinates in the cartesian plane, and a window on the plane and
  returns the appropriate pixel coordinates."
  [[x y] {:keys [bottom-left zoom]}])

(defn adjust-origin
  "Given an origin, a centre of zoom and a zoom scale, return the new
  origin."
  [[x y] [zx zy] delta-z]
  [(/ (- zx x) delta-z) (/ (- zy y) delta-z)])

(defn adjust-zoom [z delta]
  ;; REVIEW: This may not be ideal...
  (+ z delta))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Drawing
;; TODO: Use the specs to validate shapes. There's too much adhoc coordination
;; as it stands.
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmulti draw (fn [ctx vo] (:type vo)))

(defmethod draw :default [_ _] nil)

(defmethod draw :composite
  [ctx vo]
  (doseq [sub-vo (:shapes vo)]
    (draw ctx sub-vo)))

(defmethod draw :line
  [ctx {[x1 y1] :p [x2 y2] :q}]
  (.moveTo ctx x1 y1)
  (.lineTo ctx x2 y2))

(defmethod draw :circle
  [ctx {r :r [x y] :p}]
  (.moveTo ctx (+ r x) y)
  (.arc ctx x y r 0 (* 2 js/Math.PI)))

(defmethod draw :rectangle
  [ctx {[x1 y1] :p [x2 y2] :q}]
  (.moveTo ctx x1 y1)
  (.rect ctx x1 y1 (- x2 x1) (- y2 y1)))

(defn draw! [ctx vo]
  (.beginPath ctx)
  (draw ctx vo)
  (.stroke ctx)
  (.closePath ctx))
