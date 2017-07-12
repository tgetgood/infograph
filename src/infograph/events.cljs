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
   (assoc db :canvas-input-mode mode)))

(re-frame/reg-event-db
 :remove-vo
 (fn [db [_ k]]
   (update db :canvas dissoc k)))

(re-frame/reg-event-db
 :add-vo
 (fn [db [_ [k vo]]]
   (update db :canvas assoc k vo)))

(re-frame/reg-event-fx
 ::resize-canvas
 (fn [_ _]
   {::resize-canvas! true}))

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
 (fn [_]
   (canvas/set-canvas-size! (canvas/canvas))))

;;;;; Subscriptions

(re-frame/reg-sub
 :current-shape
 (fn [db _]
   (:current-shape db)))

(re-frame/reg-sub
 :data
 (fn [db _]
   (:data db)))

(re-frame/reg-sub
 :input-mode
 (fn [db _]
   (:canvas-input-mode db)))

(re-frame/reg-sub
 :canvas-raw
 (fn [db _]
   (:canvas db)))

(re-frame/reg-sub
 :input
 (fn [db _]
   (:input db)))

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
