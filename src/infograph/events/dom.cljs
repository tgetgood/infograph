(ns infograph.events.dom
  (:require [clojure.string :as string]
            [infograph.canvas :as canvas]
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
  ;; REVIEW: Laziness incarnate.
  [m]
  (into {}
        (mapcat (fn [[ks v]]
                  (map (fn [k] [k v]) ks))
                m)))

(def event-map
  (keysmap
   {[:mouse-down :touch-start] [::stroke-start]
    [:mouse-move :touch-move]  [::move]
    [:mouse-up :touch-end]     [::stroke-end]
    [:click]                   [::click]
    [:drop]                    [::drop]
    [:drag-over]               [::drag]
    [:wheel]                   [::zoom]}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; Event Locating
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn classify-event [ev]
  (let [t (.-type ev)]
    (cond
      (string/starts-with? t "mouse") :mouse
      (string/starts-with? t "touch") :touch
      :else (get events-js->clj t))))

(defn get-handlers [evt]
  (event-map evt))

(defn base-location [w ev]
  (window/pixel-clicked w ev))

(defmulti event-location (fn [w ev] (classify-event ev)) :default :mouse)

(defmethod event-location :mouse
  ;; Also covers ;wheel and :click
  [w ev]
  (base-location w ev))

(defmethod event-location :touch
  [w ev]
  (when-let [tev (aget (.-touches ev) 0)]
    (base-location w tev)))

(defmethod event-location :drag-over
  [w ev]
  (window/coproject w [(.-pageX ev) (.-pageY ev)]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; Events
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(re-frame/reg-event-db
 ::click
 (fn [db [_ ev]]
   (let [w (get-in db [:canvas :window])]
     #_(.log js/console (window/coproject w (event-location w ev)))
     db)))

(re-frame/reg-event-db
 ::zoom
 (fn [db [_ ev]]
   (let [w (get-in db [:canvas :window])
         zc (event-location w ev)
         dz (.-deltaY ev)]
     (-> db
         (update-in [:canvas :window] window/zoom-window dz zc)))))

(re-frame/reg-event-db
 ::drag
 (fn [db [_ ev]]
   (let [w (get-in db [:canvas :window])
         loc (event-location w ev)]
     (assoc-in db [:input :drag-position] loc))))

(defn- in-stroke? [db]
  (and (nil? (get-in db [:input :strokes 0 :end]))
       (not (nil? (get-in db [:input :strokes 0 :current])))
       (not (nil? (get-in db [:input :strokes 0 :start])))))

(defn- scale-dist [w l1 l2]
  (mapv - (window/coproject w l1) (window/coproject w l2)))

(re-frame/reg-event-db
 ::move
 (fn [db [_  ev]]
   (let [mode (get-in db [:canvas :input-mode])
         w (get-in db [:canvas :window])
         loc (event-location w ev)
         old (get-in db [:input :strokes 0 :current])]
     (if (= mode :grab)
       (cond-> (assoc-in db [:input :strokes 0 :current] loc)
         (and old (in-stroke? db))
         (update-in [:canvas :window] window/pan-window
                    (scale-dist w old loc)))
       (assoc-in db [:input :strokes 0 :current] (window/coproject w loc)))))) 

(re-frame/reg-event-db
 ::stroke-start
 (fn [db [_ ev]]
   (let [mode (get-in db [:canvas :input-mode])
         w (get-in db [:canvas :window])
         loc (window/coproject w (event-location w ev))]
     (let [constructor (get shapes/construction-map mode)]
       (cond-> (assoc-in db [:input :strokes 0] {:start loc})
         (not= mode :grab)
         (update-in [:canvas :shape] shapes/conj-shape (constructor loc)))))))

(re-frame/reg-event-db
 ::stroke-end
 (fn [db [_ ev]]
   (let [mode (get-in db [:canvas :input-mode])
         w (get-in db [:canvas :window])
         loc (window/coproject w (event-location w ev))]
     (cond-> (assoc-in db [:input :strokes 0 :end] loc)
       (not= mode :grab)
       (update-in [:canvas :shape] shapes/instantiate db)))))

(re-frame/reg-event-fx
 ::dom-event
 (fn [{[_ evt val] :event}]
   (let [evs (get-handlers evt)]
     {:dispatch-n (map (fn [k] [k val]) evs)})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; Canvas
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- handler [ev]
  (.preventDefault ev)
  (.persist ev)
  (let [evtype (events-js->clj (.-type ev))]
    (re-frame/dispatch [::dom-event evtype ev])))

(def canvas-event-handlers
  (into {}
        (map (fn [t] [(ev->handler t) handler])
          (keys events-clj->js))))
