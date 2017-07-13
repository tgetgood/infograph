(ns infograph.canvas)

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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Canvas Manipulation
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn canvas []
  ;;FIXME: This won't do very shortly
  (.getElementById js/document "the-canvas"))

(defn get-ctx []
  (when-let [canvas (canvas)]
    (.getContext canvas "2d")))

;; HACK: There must be a more reasonable way to compute these.
(defn width [] (quot (.-innerWidth js/window) 2))
(defn height [] (.-innerHeight js/window))

(defn set-canvas-size! [canvas]
  (set! (.-width canvas) (- (width) 10))
  (set! (.-height canvas) (- (height) 10)))

(defn clear! [ctx]
  (.clearRect ctx 0 0 (width) (height)))

(defn drag-location [e]
  [(.-pageX e) (.-pageY e)])

(defn click-location [e]
  (let [c (canvas)]
    [(- (.-pageX e) (.-offsetLeft c))
     (- (.-pageY e) (.-offsetTop c))]))

(defn touch-location [e]
  (when-let [t (aget (.-touches e) 0)]
    (click-location t)))
