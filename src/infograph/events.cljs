(ns infograph.events
  (:require [infograph.canvas :as canvas]
            [infograph.db :as db]
            [infograph.events.dom :as dom-events]
            [infograph.shapes :as shapes]
            [re-frame.core :as re-frame]))

(defn q [name]
  (keyword :infograph.events name)) 

;;;;; Events

(re-frame/reg-event-db
 :initialize-db
 (fn  [_ _]
   db/default-db))

(re-frame/reg-event-db
 :update-data-value
 ;; TODO: history and undo
 (fn [db [_ p v]]
   (assoc-in db (cons :data p) v)))

(re-frame/reg-event-db
 :set-input-mode
 (fn [db [_ mode]]
   (assoc-in db [:canvas :input-mode] mode)))

;; TODO: Listen for window resizing events.
;; FIXME: This is impure, but where's the right place to access the DOM?
(re-frame/reg-event-fx
 ::resize-canvas
 (fn [{:keys [db]}]
   (let [canvas (canvas/canvas)
         [width height :as dim] (canvas/canvas-container-dimensions)]
     {:db (update-in db [:canvas :window] assoc :width width :height height)
      ::resize-canvas! [canvas dim]})))

(re-frame/reg-event-fx
 ::redraw-canvas
 (fn [{[_ d] :event}]
   {::redraw-canvas! d}))

(re-frame/reg-event-fx
 ::dom-event
 (fn [{[_ evt val] :event}]
   (let [evs (dom-events/get-handlers evt)]
     {:dispatch-n (map (fn [k] [k val]) evs)})))

;;;;; FX
 
(re-frame/reg-fx
 ::redraw-canvas!
 (fn [drawing]
   (when drawing
     (when-let [ctx (canvas/get-ctx)]
       (canvas/clear! ctx)
       (canvas/draw! ctx drawing)))))

(re-frame/reg-fx
 ::resize-canvas!
 (fn [[canvas dimensions]]
   (canvas/set-canvas-size! canvas dimensions)))

;;;;; Subscriptions

(re-frame/reg-sub
 :data
 (fn [db _]
   (:data db)))

(re-frame/reg-sub
 :input-mode
 (fn [db _]
   (get-in db [:canvas :input-mode])))

(re-frame/reg-sub
 :canvas-raw
 (fn [db _]
   (get-in db [:canvas :shape])))

(re-frame/reg-sub
 :input
 (fn [db _]
   (:input db)))

(re-frame/reg-sub
 :drag-position
 (fn [db _]
   (get-in db [:input :drag-position])))

(re-frame/reg-sub
 :canvas
 (fn [_ _]
   [(re-frame/subscribe [:canvas-raw])
    (re-frame/subscribe [:data])
    (re-frame/subscribe [:input])])
 (fn [[canvas data input]]
   (-> canvas
       (shapes/instantiate data)
       (shapes/react input))))
