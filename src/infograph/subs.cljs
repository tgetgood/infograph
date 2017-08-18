(ns infograph.subs
  (:require [infograph.db :as db]
            [infograph.shapes :as shapes]
            [re-frame.core :as re-frame]))

;; HACK: I don't want to store the last thing drawn in the app state but need
;; it...
(def last-draw (atom nil))

(re-frame/reg-sub
 :data-focus
 (fn [db _]
   (-> db :data :focus)))

(re-frame/reg-sub
 :shape-focus
 (fn [db _]
   (:property-window db)))

(re-frame/reg-sub
 :input-mode
 (fn [db _]
   (get-in db [:canvas :input-mode])))

(re-frame/reg-sub
 :canvas-raw
 (fn [db _]
   (get-in db [:canvas :shape])))

(re-frame/reg-sub
 :inst-data
 (fn [db _]
   (db/inst-data db)))

(re-frame/reg-sub
 :drag-position
 (fn [db _]
   (get-in db [:input :drag-position])))

(re-frame/reg-sub
 :window
 (fn [db _]
   (db/window db)))

(re-frame/reg-sub
 :r2-canvas
 (fn [_ _]
   [(re-frame/subscribe [:canvas-raw])
    (re-frame/subscribe [:inst-data])])
 (fn [[canvas data]]
   (shapes/instantiate canvas data)))

(re-frame/reg-sub
 :canvas
 (fn [_ _]
   [(re-frame/subscribe [:r2-canvas])
    (re-frame/subscribe [:window])])
 (fn [[canvas window]]
   (reset! last-draw (shapes/project canvas window))))
