(ns infograph.canvas)

(defn canvas []
  ;;FIXME: This won't do very shortly
  (.getElementById js/document "the-canvas"))

(defn get-ctx []
  (when-let [canvas (canvas)]
    (.getContext canvas "2d")))

(defn width [] (quot (.-innerWidth js/window) 2))
(defn height [] (.-innerHeight js/window))

(defn set-canvas-size! [canvas]
  (set! (.-width canvas) (- (width) 10))
  (set! (.-height canvas) (- (height) 10)))

(defn clear! [ctx]
  (.clearRect ctx 0 0 (width) (height)))

(defmulti draw* (fn [_ x] (:type x)))

(defmethod draw* :default [_ _])

(defmethod draw* :line
  [ctx {[x1 y1] :start [x2 y2] :end}]
  (.moveTo ctx x1 y1)
  (.lineTo ctx x2 y2))

(defn draw! [ctx path]
  (.beginPath ctx)
  (draw* ctx path)
  (.stroke ctx)
  (.closePath ctx))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Canvas Event Handling
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn click-location [e]
  (let [c (canvas)]
    [(- (.-pageX e) (.-offsetLeft c))
     (- (.-pageY e) (.-offsetTop c))]))

(defmulti canvas-handler (fn [mode e] mode) :default :none)

(defmethod canvas-handler :none [_ _])

(defmethod canvas-handler :line
  [_ e]

  (-> e click-location js/console.log))

(defn canvas-click-handler
  "Handling clicks on canvas basically involves writing your own gui system from
  scratch. Difficult? yes. Exciting? yes. Useful? I certainly hope so."
  [mode]
  (fn [e]
    (canvas-handler mode e)))
