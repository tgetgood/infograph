(ns infograph.events
  (:require [infograph.canvas :as canvas]
            [infograph.db :as db]
            [infograph.events.dom :as dom-events]
            [infograph.shapes :as shapes]
            [infograph.window :as window]
            [infograph.window.protocols :as wp]
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
 (fn [{db :db [_ canvas] :event}]
   (let [[width height :as dim] (canvas/canvas-container-dimensions)]
     {:db (update-in db [:canvas :window] assoc :width width :height height)
      ::resize-canvas! [canvas dim]})))

(re-frame/reg-event-fx
 ::redraw-canvas
 (fn [{[_ elem {:keys [window content]}] :event}]
   (let [window (-> elem canvas/context (window/create window content))]
     {::redraw-canvas! window})))

(re-frame/reg-event-fx
 ::dom-event
 (fn [{[_ evt val] :event}]
   (let [evs (dom-events/get-handlers evt)]
     {:dispatch-n (map (fn [k] [k val]) evs)})))

;;;;; FX
 
(re-frame/reg-fx
 ::redraw-canvas!
 (fn [window]
   (wp/refresh window)))

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
 :inst-data
 (fn [_ _]
   [(re-frame/subscribe [:data])
    (re-frame/subscribe [:input])])
 (fn [[data input]]
   {:data data :input input}))

(re-frame/reg-sub
 :drag-position
 (fn [db _]
   (get-in db [:input :drag-position])))

(re-frame/reg-sub
 :window
 (fn [db _]
   (get-in db [:canvas :window])))

(re-frame/reg-sub
 :canvas
 (fn [_ _]
   [(re-frame/subscribe [:canvas-raw])
    (re-frame/subscribe [:inst-data])
    (re-frame/subscribe [:window])])
 (fn [[canvas data window]]
   {:window window
    :content (shapes/instantiate canvas data)}))
