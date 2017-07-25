(ns infograph.events.dom
  (:require [infograph.canvas :as canvas]
            [infograph.shapes :as shapes]
            [infograph.window :as window]
            [re-frame.core :as re-frame]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; DOM Event Normalisation
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn ev->handler [ev]
  (keyword (str "on-" (name ev))))

(def events-clj->js
  {:wheel       "wheel"
   :click       "click"
   :mouse-down  "mousedown"
   :mouse-move  "mousemove"
   :mouse-up    "mouseup"
   :touch-start "touchstart"
   :touch-move  "touchmove"
   :touch-end   "touchend"
   :drag-over   "dragover"
   :drop        "drop"})

(def events-js->clj
  (into {}
        (map (comp (partial into []) reverse) events-clj->js)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;; Event Handling
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn keysmap
  "Shorthand for creating maps where many keys share the same value.
  Each key is assumed to be a collection which is expanded into separate keys
  with the same value.
  Example: (keysmap {[:a :b] :c [:d] :e}) => {:a :c, :b :c, :d :e}"
  [m]
  (into {}
        (mapcat (fn [[ks v]]
                  (map (fn [k] [k v]) ks))
                m)))

(defn drop-path [c ev]
  )

(def event-processing
  (keysmap
   {[:mouse-down :mouse-move :mouse-up]   canvas/click-location
    [:touch-start :touch-move :touch-end] canvas/touch-location
    [:drop]                               drop-path
    [:click]                              canvas/click-location
    [:drag-over]                          canvas/drag-location
    [:wheel]                              canvas/analyse-zoom}))

(def event-map
  (keysmap
   {[:mouse-down :touch-start] [::stroke-start]
    [:mouse-move :touch-move]  [::move]
    [:mouse-up :touch-end]     [::stroke-end]
    [:click]                   [::click]
    [:drop]                    [::drop]
    [:drag-over]               [::drag]
    [:wheel]                   [::zoom]}))

(defn get-handlers [evt]
  (event-map evt))

(defprotocol EventWindow
  (dispatch [this ev])
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; Events
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(re-frame/reg-event-db
 ::click
 (fn [db [_ loc]]
   (.log js/console loc)
   db))

(re-frame/reg-event-db
 ::zoom
 (fn [db [_ [zc dz]]]
   (-> db
       (update-in [:canvas :window] window/zoom-window dz zc))))

(re-frame/reg-event-db
 ::drag
 (fn [db [_ loc]]
   (assoc-in db [:input :drag-position] loc)))

(defn translate [p q]
  (mapv + p q))

(defn- in-stroke? [db]
  (and (nil? (get-in db [:input :strokes 0 :end]))
       (not (nil? (get-in db [:input :strokes 0 :current])))
       (not (nil? (get-in db [:input :strokes 0 :start])))))

(re-frame/reg-event-db
 ::move
 (fn [db [_  loc]]
   (let [mode (get-in db [:canvas :input-mode])
         old (get-in db [:input :strokes 0 :current])]
     (cond-> (assoc-in db [:input :strokes 0 :current] loc)
       (and (= mode :grab) (in-stroke? db))
       (update-in [:canvas :window] window/pan-window (mapv - old loc))))))

(re-frame/reg-event-db
 ::stroke-start
 (fn [db [_ loc]]
   (let [mode (get-in db [:canvas :input-mode])]
     (let [constructor (get shapes/construction-map mode)]
       (cond-> (assoc-in db [:input :strokes 0] {:start loc})
         (not= mode :grab)
         (update-in [:canvas :shape] conj (constructor loc)))))))

(re-frame/reg-event-db
 ::stroke-end
 (fn [db [_ loc]]
   (let [mode (get-in db [:canvas :input-mode])]
     (cond-> (assoc-in db [:input :strokes 0 :end] loc)
       (not= mode :grab)
       (update-in [:canvas :shape] shapes/instantiate db)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; Canvas
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- handler [ev]
  (.preventDefault ev)
  (let [evtype (events-js->clj (.-type ev))
        processed (when (contains? event-processing evtype)
                    ((get event-processing evtype) ev))]
    (re-frame/dispatch [:infograph.events/dom-event evtype processed])))

(def canvas-event-handlers
  (into {}
        (map (fn [t] [(ev->handler t) handler])
          (keys events-clj->js))))
