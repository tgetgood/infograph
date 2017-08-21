(ns infograph.events
  (:require [infograph.canvas :as canvas]
            [infograph.db :as db]
            [infograph.shapes :as shapes]
            [infograph.subs :as subs]
            [re-frame.core :as re-frame]))

;;;;; Hacky bits

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
   (assoc-in db (concat [:data :data] p) v)))

(re-frame/reg-event-db
 :set-input-mode
 (fn [db [_ mode]]
   (assoc-in db [:canvas :input-mode] mode)))

(re-frame/reg-event-fx
 ::resize-canvas
 (fn [{db :db [_ canvas] :event}]
   (let [[width height :as dim] (canvas/canvas-container-dimensions)
         offset                 (canvas/canvas-container-offset)]
     (array-map
      :db              (update-in db db/window-path assoc
                                  :width width :height height :offset offset)
      ::resize-canvas! [canvas dim]
      ::redraw-canvas! [(canvas/context canvas) @subs/last-draw]))))

(re-frame/reg-event-fx
 ::redraw-canvas
 (fn [{[_ elem content] :event}]
   (let [ctx (canvas/context elem)]
     {::redraw-canvas! [ctx content]})))

;;; User Input Events

;; HACK: If this works out we should definitely use records and implement
;; IAssociative
(defn hack-assoc [coll k v]
  (cond
    (satisfies? IAssociative coll) (assoc coll k v)
    (set? coll)                    (-> coll (disj k) (conj v))
    :else                          (.error js/console "hack-assoc " coll)))

(defn hack-assoc-in [coll [k & ks] v]
  (if ks
    (hack-assoc coll k (hack-assoc-in (get coll k) ks v))
    (hack-assoc coll k v)))

(re-frame/reg-event-db
 ::property-drop
 (fn [db [_ drop-path query-path]]
   (update-in db [:canvas :shape]
              hack-assoc-in query-path (shapes/connection drop-path))))

(re-frame/reg-event-db
 ::property-edit
 (fn [db [_ qp v]]
   (update-in db [:canvas :shape]
              hack-assoc-in qp v)))

(re-frame/reg-event-db
 ::toggle-data
 (fn [db [_ path]]
   (let [q (concat [:data :focus] (interleave (repeat :children) path) [:open?])]
     (update-in db q not))))

;;;;; FX

(re-frame/reg-fx
 ::redraw-canvas!
 (fn [[ctx content]]
   (shapes/draw! ctx content)))

(re-frame/reg-fx
 ::resize-canvas!
 (fn [[canvas dimensions]]
   (canvas/set-canvas-size! canvas dimensions)))
