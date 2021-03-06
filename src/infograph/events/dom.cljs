(ns infograph.events.dom
  (:require [clojure.string :as string]
            [infograph.db :as db]
            [infograph.locator :as locator]
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
   :drag-end    "dragend"
   :drag-enter  "dragenter"
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
    [:drop :drag-end]          [::drop]
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
  (window/coproject w (window/invert w (window/pixel-clicked w ev))))

(defmulti event-location (fn [w ev] (classify-event ev)) :default :mouse)

(defmethod event-location :mouse
  [w ev]
  (base-location w ev))

;; HACK: It's not a good idea to function in two modes at once. We need to stop
;; using cartesian coords in some places and cg coords in others.
(defmethod event-location :click
  [w ev]
  (window/pixel-clicked w ev))

(defmethod event-location :touch
  [w ev]
  (when-let [tev (aget (.-touches ev) 0)]
    (base-location w tev)))

(defmethod event-location :drag-over
  [w ev]
  ;; Dragging is dealt with in pixel distance.
  (window/pixel-clicked w ev))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; Util
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- scale-dist [w l1 l2]
  (mapv - (window/coproject w l1)
        (window/coproject w l2)))

(defn- in-stroke? [db]
  (and (nil? (get-in db [:input :strokes 0 :end]))
       (not (nil? (get-in db [:input :strokes 0 :current])))
       (not (nil? (get-in db [:input :strokes 0 :start])))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; Events
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(re-frame/reg-event-db
 ::drop
 (fn [db [_ ev]]
   (let [w (db/window db)
         data (db/inst-data db)
         shapes (shapes/instantiate (get-in db (conj db/current-canvas :shapes))
                                    data)
         loc (event-location w ev)])
   db))

(re-frame/reg-event-db
 ::drag
 (fn [db [_ ev]]
   (let [w (db/window db)
         loc (event-location w ev)]
     (assoc-in db [:input :drag-position] loc))))

(re-frame/reg-event-db
 ::reset-drag
 (fn [db _]
   (update db :input dissoc :drag-position)))

(re-frame/reg-event-db
 ::click
 (fn [db [_ ev]]
   (let [w (db/window db)
         loc (event-location w ev)]
     (assoc-in db [:canvas :last-click] loc))))

(re-frame/reg-event-db
 ::zoom
 (fn [db [_ ev]]
   (let [w (db/window db)
         zc (event-location w ev)
         dz (.-deltaY ev)]
     (-> db
         (update-in db/window-path window/zoom-window dz zc)))))

(re-frame/reg-event-db
 ::move
 (fn [db [_  ev]]
   (let [mode (get-in db [:canvas :input-mode])
         w (db/window db)]
     (if (= mode :grab)
       (let [loc (window/invert w (window/pixel-clicked w ev))
             old (get-in db [:input :strokes 0 :current])]
         (cond-> (assoc-in db [:input :strokes 0 :current] loc)
           (and old (in-stroke? db))
           (update-in db/window-path window/pan-window
                      (scale-dist w old loc))))
       (let [loc (event-location w ev)]
         (assoc-in db [:input :strokes 0 :current] loc))))))

(re-frame/reg-event-db
 ::stroke-start
 (fn [db [_ ev]]
   (let [mode (get-in db [:canvas :input-mode])
         w (db/window db)
         loc (event-location w ev)]
     (let [constructor (get shapes/construction-map mode)]
       (cond-> (assoc-in db [:input :strokes 0] {:start loc})
         (not= mode :grab)
         (update-in db/current-canvas shapes/conj-shape (constructor loc)))))))

(defmulti valid? (fn [s] (:type s)))

(defmethod valid? :default [_] true)

(defmethod valid? :line
  [{{u :x v :y} :p {s :x t :y} :q}]
  (not (some nil? [s t u v])))

(defmethod valid? :rectangle
  [{{a :x b :y} :p {s :x t :y} :w {u :x v :y} :h}]
  (not (some nil? [a b s t u v])))

(defmethod valid? :circle
  [{{x :x y :y} :c {r :v} :r}]
  (and (not (some nil? [x y r]))
       (js/isFinite r)))

;; FIXME: This is a mess.
(defn insta-clean [frame data]
  (let [inst (shapes/instantiate frame data)]
    (update inst :shapes
            #(into #{} (filter valid? %)))))

(re-frame/reg-event-db
 ::stroke-end
 (fn [db [_ ev]]
   (let [mode (get-in db [:canvas :input-mode])
         ;; Make sure we only update the input dependencie
         data (db/input db)
         w (db/window db)
         loc (event-location w ev)]
     (cond-> (assoc-in db [:input :strokes 0 :end] loc)
       (not= mode :grab)
       (update-in db/current-canvas insta-clean data)))))

(re-frame/reg-event-fx
 ::dom-event
 (fn [{[_ evt val] :event}]
   (let [evs (get-handlers evt)]
     {:dispatch-n (map (fn [k] [k val]) evs)})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; Canvas
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- handler [ev]
  (.persist ev)
  (.preventDefault ev)
  (let [evtype (events-js->clj (.-type ev))]
    (re-frame/dispatch [::dom-event evtype ev])))

(def canvas-event-handlers
  (into {}
        (map (fn [t] [(ev->handler t) handler])
          (keys events-clj->js))))
