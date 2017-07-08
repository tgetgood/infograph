(ns infograph.canvas
  (:require [infograph.shapes :as shapes]
            [re-frame.core :as re-frame]))

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

;; (defmulti draw* (fn [_ x] (:type x)))

;; (defmethod draw* :default [_ _])

;; (defmethod draw* :line
;;   [ctx {[x1 y1] :start [x2 y2] :end}]
;;   (.moveTo ctx x1 y1)
;;   (.lineTo ctx x2 y2))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Protocols
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defprotocol Shape
  "Protocol that must be implemented by all shapes."
  ;; Expose the inner structure in some format
  (properties [this])
  ;; Create a concrete visual object from the given data and this abstract
  ;; visual object
  (instantiate [this data]))

(defprotocol Draw
  (draw [this ctx]))

;; REVIEW: Do we parse the events in the handler, here, or in an intermediate
;; layer? Here would couple us to the events, but the dom changes very
;; slowly. In the event handler could prevent annoyances with the proxy and
;; virtual events.

;; REVIEW: Should these be named after what they do or what they handle? Should
;; we in fact have one protocol per event? Per event family?

;; Events to be handled:
;;
;; wheel
;; 
(defprotocol Wheel
  (wheel [this e]))

(defprotocol Motion
  (start [this loc])
  (move [this loc])
  (end [this loc]))

(defprotocol IDroppable
  (f [_]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;; Shapes
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn create-line [props]
  (reify
    Shape
    (properties [_] props)
    (instantiate [_ data] (shapes/instantiate props data))

    Draw
    (draw [this ctx]
      (let [{[x1 y1] :start [x2 y2] :end} (shapes/instantiate this data)]
        (.moveTo ctx x1 y1)
        (.lineTo ctx x2 y2)))))

(defn line-constructor
  ([p] (line-constructor [p p]))
  ([p q]
   (reify
     Draw
     (draw [_ ctx]
       (let [[x1 y1] p
             [x2 y2] q]
         ;; REVIEW: I think I need to return this as data and write a separate
         ;; renderer. Use case: Drawing these lines might be more obvious if you
         ;; saw a greyed out rectangle with the line as diagonal, or some such.
        (.moveTo ctx x1 y1)
        (.lineTo ctx x2 y2)))
     
     Motion
     (move [this loc]
       (line-constructor p loc))
     (end [_ loc]
       (create-line {:start p :end q :type :line})))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;; Handy Helper
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn draw! [ctx vo]
  (.beginPath ctx)
  (draw vo ctx)
  (.stroke ctx)
  (.closePath ctx))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Canvas Event Handling
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn click-location [e]
  (let [c (canvas)]
    [(- (.-pageX e) (.-offsetLeft c))
     (- (.-pageY e) (.-offsetTop c))]))

;; Layer of indirection 1 maps dom events on the canvas element to abstractions
;; that I build for my own convenience. This allows the visual objects to
;; subscribe to higher level services.'
;;
;; Layer 2 takes those abstractions and maps them to the visual objects which
;; subscribe to them.
;;
;; That seems reasonable, no?
;;
;; Event Abstractions:
;;
;; Motion: This consists of mouse-down, touch-start, move and up/end. Basically
;; what you can think of as a stroke. Maybe stroke is a better name.
;;
;; Zoom: scrolling with wheel or two fingers on trackpad/touchscreen
;;

(def ^:dynamic *mode* :none)

(def event-map
  (atom {:wheel #{}
         :mouse-down #{}}))

(def vo-map (atom {:motion #{}}))

;; Now what should cb be? 
(defn register! [m ev cb]
  (swap! m update ev conj cb))

(defn unregister! [m ev cb]
  (swap! m update ev disj cb))

(defn handler [type mode e]
  (binding [*mode* mode]
    (doseq [cb (get @event-map type)]
      (cb type e))))

(defprotocol AbstractEvent
  (can-invoke? [this o])
  (handle [this t ev]))

(def motion
  ;; if receives :mouse-down or :touch-start do:
  (reify
    AbstractEvent
    (can-invoke? [_ o] (satisfies? Motion o))
    (handle [_ type ev]
      (let [loc (click-location ev)]
        (doseq [o (get @vo-map :motion)]
          (cond
            (contains? #{:mouse-down :touch-start} type)
            (start o loc)

            (contains? #{:mouse-move :touch-move} type)
            (move o loc)

            (contains? #{:mouse-up :touch-end} type)
            (end o loc)))))))


