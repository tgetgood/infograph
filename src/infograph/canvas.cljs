(ns infograph.canvas
  (:require [clojure.string :as string]
            [infograph.shapes :as shapes]
            [infograph.uuid :as uuid]
            [re-frame.core :as re-frame]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Protocols
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defprotocol EventManager
  "Event processor"
  (handle [this type ev])
  (register! [this o])
  (unregister! [this o]))

(defprotocol Shape
  "Protocol that must be implemented by all shapes."
  ;; Expose the inner structure in some format
  (properties [this])
  ;; Create a concrete visual object from the given data and this abstract
  ;; visual object
  (instantiate [this data]))

(defprotocol Draw
  "Useless docstrings"
  (draw [this ctx]))

(defprotocol Wheel
  (wheel [this e]))

(defprotocol Motion
  (start [this loc])
  (move [this loc])
  (end [this loc]))

(defprotocol IDroppable
  (f [_]))

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

(defn draw! [ctx vos]
  (doseq [vo vos]
    (.beginPath ctx)
    (draw vo ctx)
    (.stroke ctx)
    (.closePath ctx)))

(defn click-location [e]
  (let [c (canvas)]
    [(- (.-pageX e) (.-offsetLeft c))
     (- (.-pageY e) (.-offsetTop c))]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Canvas Event Handling
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; By allowing event handlers to register to one another, we can have multiple
;; levels of indirection and create stream processing graphs. Currently there
;; are 3 levels.
;;
;; Level one is the canvas-event-manager. It handles all of the DOM events and
;; dispatches them to interested parties.
;;
;; Level two are the higher order event-managers. They are responsible for
;; grouping and/or preprocessing events. As currently implemented they come with
;; protocols and the event-manager constructor asserts that objects registered
;; satisfy the protocol. That may go.
;;
;; The third layer are the visual objects and visual constructors
;; themselves. They can subscribe directly to the canvas-event-manager. I don't
;; yet know whether that's a good thing or a bad thing.
;;
;;
;; Currently the canvas-event-manager treats calls to register! differently
;; because presumably no one want to listen for any possible event. It might
;; make sense to change the signature of handle and allow objects subscribing to
;; event managers to specify which methods they're interested in having
;; invoked. I feel like there's a much simpler structure for that though if it
;; becomes necessary. 
;;
;;
;; So on further thought, this is a pretty crummy solution. The basic idea is
;; good, but the registration creates too much coupling. Should be using queues
;; or channels instead. Also the mode should be looked up via
;; re-frame/subscribe, we don't need dynamic binding. Anyway this is good enough
;; for now. Get something that can be used.

(def ^:dynamic *mode* :none)

(defn clear-value [obj]
  (fn [m]
    (into {} (map (fn [[k v]] [k (disj v obj)])))))

(def canvas-event-manager
  (let [event-map (atom {})]
    (reify
      EventManager
      (handle [_ type ev]
        (doseq [obj (get @event-map type)]
          (handle obj type ev)))
      (register! [_ [types obj]]
        (doseq [type types]
          (swap! event-map update type conj obj)))
      (unregister! [_ obj]
        (swap! event-map (clear-value obj))))))

#_(defn handler [type mode ev]
  (binding [*mode* mode]
    (handle canvas-event-manager type ev)))

(defn- event-manager
  [protocol dispatch-map]
  (let [registees (atom {})
        this (reify
               EventManager
               (register! [_ [k obj]]
                 #_(assert (satisfies? protocol obj))
                 (swap! registees assoc k obj))
               (unregister! [_ k]
                 ;; Unregistration will be a rare event, so presumably quick
                 ;; iteration is more important than efficient removal
                 (swap! registees dissoc k))
               (handle [_ type ev]
                 (let [f (get dispatch-map type)]
                   (doseq [obj (vals @registees)]
                     ;; Without reflection I can't guarantee that the methods
                     ;; take two args. Or is there another way?
                     (f obj ev)))))]
    (register! canvas-event-manager [(keys dispatch-map) this])
    this))

(defn lazy-map
  [& kvs]
  {:pre [(even? (count kvs))]}
  (into {}
        (mapcat (fn [[ks v]] (map (fn [k] [k v]) ks))
                (partition 2 kvs))))

(def motion
  (event-manager
   Motion
   (lazy-map
    #{:mouse-down :touch-start} #(start %1 (click-location %2))
    #{:mouse-move :touch-move} #(move %1 (click-location %2))
    #{:mouse-up :touch-end} #(end %1 (click-location %2)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;; Shapes
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn line [props]
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
  ([p] (line-constructor p p))
  ([p q]
   (let [q (atom q)
         id (uuid/squuid)
         this
         (reify
           Draw
           (draw [_ ctx]
             (let [[x1 y1] p
                   [x2 y2] @q]
               ;; REVIEW: I think I need to return this as data and write a
               ;; separate renderer. Use case: Drawing these lines might be more
               ;; obvious if you saw a greyed out rectangle with the line as
               ;; diagonal, or some such.
               (.moveTo ctx x1 y1)
               (.lineTo ctx x2 y2)))
           
           Motion
           (start [_ _])
           (move [_ loc]
             (reset! q loc))
           (end [_ loc]
             (unregister! motion id)
             (re-frame/dispatch [:remove-vo id])
             (re-frame/dispatch [:add-vo [(uuid/squuid) (line {:start p
                                                          :end loc
                                                          :type :line})]])))]
     (register! motion [id this])
     [id this])))


(def constructor
  (event-manager
   Motion
   (lazy-map
    [:mouse-down :touch-start]
    (fn [_ ev]
      (.log js/console ev)
      (let [loc (click-location ev)]
        (cond
          (= *mode* :line) (re-frame/dispatch [:add-vo (line-constructor loc)])
          :else 3))))))

(register! constructor [1 (reify Motion (start [_ _]) (move [_ _]) (end [_ _]))])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; Attempt #2
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmulti process-event (fn [ev] (.-type ev)))
;; TODO: unify :mouse-down & "mousedown", etc.. Why was it a good idea to create
;; a new taxonomy?

(defn stringify-event-type [k]
  (string/replace (name k) #"-" ""))

(def event-processing
  {[:mouse-down :touch-start :mouse-move :mouse-up :touch-move :touch-end]
   click-location})

(doseq [[ks f] event-processing]
  (doseq [k ks]
    (defmethod process-event (stringify-event-type k) [ev] (f ev))))

(defn handler [ev]
  (.log js/console (process-event ev)))
