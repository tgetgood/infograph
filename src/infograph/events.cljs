(ns infograph.events
  (:require [infograph.canvas :as canvas]
            [infograph.db :as db]
            [infograph.shapes :as shapes]
            [re-frame.core :as re-frame]))

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

(re-frame/reg-event-fx
 ::resize-canvas
 (fn [_ _]
   {::resize-canvas! true}))

(re-frame/reg-event-fx
 ::redraw-canvas
 (fn [{[_ d] :event :as a}]
   {::redraw-canvas! d}))

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
 :canvas
 (fn [db _]
   (vals (:canvas db))))
