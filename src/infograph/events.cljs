(ns infograph.events
  (:require [infograph.canvas :as canvas]
            [infograph.db :as db]
            [infograph.events.dom :as dom-events]
            [infograph.shapes :as shapes]
            [re-frame.core :as re-frame]))

;;;;; Hacky bits

(def last-draw (atom nil))

(defn q [name]
  (keyword :infograph.events name)) 

;;;;; Events

(re-frame/reg-event-db
 ::initialize-db
 (fn  [_ _]
   db/default-db))

(re-frame/reg-event-db
 :update-data-value
 (fn [db [_ p v]]
   (assoc-in db (cons :data p) v)))

(re-frame/reg-event-db
 :set-input-mode
 (fn [db [_ mode]]
   (assoc-in db [:canvas :input-mode] mode)))

(re-frame/reg-event-fx
 ::resize-canvas
 (fn [{db :db [_ canvas] :event}]
   (let [[width height :as dim] (canvas/canvas-container-dimensions)
         offset (canvas/canvas-container-offset)]
     (array-map
      :db (update-in db [:canvas :window] assoc
                     :width width :height height :offset offset)
      ::resize-canvas! [canvas dim]
      ::redraw-canvas! [(canvas/context canvas) @last-draw]))))

(re-frame/reg-event-fx
 ::redraw-canvas
 (fn [{[_ elem content] :event}]
   (let [ctx (canvas/context elem)]
     {::redraw-canvas! [ctx content]})))

;;;;; FX
 
(re-frame/reg-fx
 ::redraw-canvas!
 (fn [[ctx content]]
   (shapes/draw! ctx content)))

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
   (reset! last-draw (-> canvas
                         (shapes/instantiate data)
                         (shapes/project window)))))
