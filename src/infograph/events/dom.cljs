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
         shapes (shapes/instantiate (get-in db [:canvas :shape :shapes]) data)
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

;; TODO: Should probably update this to make the interior of shapes distance 0
;; from them. I.e. treat VOs as solids, not outlines.
(defn nearest-shape [w data {:keys [shapes]} loc]
  (->> shapes
       (map (fn [s]
              [(-> s
                   (shapes/instantiate data)
                   (shapes/project w)
                   (locator/dist loc))
               s]))
       (sort-by first)
       first))

(re-frame/reg-event-db
 ::click
 (fn [db [_ ev]]
   (let [w (db/window db)
         loc (event-location w ev)
         data (db/inst-data db)
         frame (get-in db [:canvas :shape])
         [d s] (nearest-shape w data frame loc)]
     (cond-> db
       (and (number? d) (< d 20)) (assoc :property-window s)))))

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
         (update-in [:canvas :shape] shapes/conj-shape (constructor loc)))))))

(defn valid-line? [s]
  ;; FIXME: This is a job for specs
  (not (some nil?
              ((apply juxt (map (fn [k] (fn [d] (get-in d k)))
                              [[:p :x] [:p :y] [:q :x] [:q :y]]))
               s))))

;; FIXME: This is a mess.
(defn insta-clean [frame data]
  (let [inst (shapes/instantiate frame data)]
    (update inst :shapes
            #(into #{}
                  (remove (fn [s]
                            (and (= :line (:type s)) (not (valid-line? s))))
                          %)))))

(re-frame/reg-event-db
 ::stroke-end
 (fn [db [_ ev]]
   (let [mode (get-in db [:canvas :input-mode])
         data (db/inst-data db)
         w (db/window db)
         loc (event-location w ev)]
     (cond-> (assoc-in db [:input :strokes 0 :end] loc)
       (not= mode :grab)
       (update-in [:canvas :shape] insta-clean data)))))

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
