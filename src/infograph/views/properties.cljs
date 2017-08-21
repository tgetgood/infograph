(ns infograph.views.properties
  (:require [cljs.reader :refer [read-string]]
            [infograph.css :as css]
            [infograph.locator :as locator]
            [infograph.shapes :as shapes]
            [re-frame.core :as re-frame]
            [reagent.core :as reagent]))

(defprotocol ValueRender
  (render-value [this] [this query]))

(defn- receive-drop [query-path ev]
  (.stopPropagation ev)
  (re-frame/dispatch [:infograph.events.dom/reset-drag])
  (let [drop-path (read-string
                   (.getData (.-dataTransfer ev) "path"))]
    (re-frame/dispatch [:infograph.events/property-drop drop-path query-path])))

(defn receive-edit [query-path ev]
  (let [val (-> ev .-target .-value js/parseInt)]
    (re-frame/dispatch [:infograph.events/property-edit query-path val])))

(defn drop-cbs [q]
  {:on-drag-over  #(.preventDefault %)
   ;; TODO: Drag hover effects
   :on-drop (partial receive-drop q)})

(defn nested-values [v r]
  [:span (str v) " " r])

(defn value-dropper [v q]
  (let [open? (reagent/atom false)]
    (fn [v q]
      [:div (drop-cbs q)
       (if @open?
         [:input {:on-change (partial receive-edit q)
                  :on-key-press #(let [c (or (.-which %) (.-keyCode %))]
                                   (when (== 13 c)
                                     (swap! open? not)))
                  :type "text"
                  :default-value v}]
         [:span {:on-click #(swap! open? not)} v])])))

(defn- pair-table [{:keys [x y]} q]
  [:table (drop-cbs q)
   [:tbody
    [:tr (drop-cbs (conj q :x))
     [:td [:span "x"]]
     [:td [value-dropper (render-value x) (conj q :x)]]]
    [:tr (drop-cbs (conj q :y))
     [:td [:span "y"]]
     [:td [value-dropper (render-value y) (conj q :y)]]]]])

;; REVIEW: Schemata are fundamentally different from projectables. Currently
;; that's dealt with by a variadic render function. What happens though when we
;; have projectables inside schemata?
(extend-protocol ValueRender
  default
  (render-value
    ([this]
     (str this))
    ([this path]
     [value-dropper (str this) path]))

  infograph.shapes.impl/Coordinate-2D
  (render-value [this q]
    (pair-table this q))

  infograph.shapes.impl/Vector-2D
  (render-value [this q]
    (pair-table this q))

  infograph.shapes.impl/Scalar
  (render-value [this q]
    [value-dropper (render-value (:v this)) (conj q :v)])

  infograph.shapes.impl/SubSchema
  (render-value [{:keys [query shape]}]
    (nested-values query (render-value shape)))

  infograph.shapes.impl/ValueSchema
  (render-value [{:keys [query]}]
    (nested-values query "")))

(defn- table-row [[k v] q]
  [:tr
   [:td [:span k]]
   [:td (render-value v (conj q k))]])

(defn map->table [m q]
  `[:table
    [:thead
     [:tr
      [:th [:span "Property"]]
      [:th [:span "Value"]]]]
    [:tbody
     ~@(map #(table-row % q) m)]])

;; TODO: Unified shape classification system.
(defn- classify [s] (:type s))

(defmulti shape-properties classify)

(defmethod shape-properties :circle
  [s]
  (select-keys s [:c :r]))

(defmethod shape-properties :line
  [s]
  (select-keys s [:p :q]))

(defmethod shape-properties :rectangle
  [s]
  (select-keys s [:p :w :h]))

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

(defn floating-property-window [w data canvas drag-position]
  (when drag-position
    (let [[x y :as loc] drag-position
          [d s]         (nearest-shape w data canvas loc)
          [ox oy]       (:offset w)]
      (if (and d (< d 20))
        [:div {:style {:position        "absolute"
                       :backgroundColor "rgba(255,255,255,0.8)"
                       :top             (+ y 20)
                       :left            (+ ox x)}}
         [map->table (shape-properties s) [:shapes s]]]
        [:div {:style {:display :none}}]))))

(defn fixed-property-window [w data raw-frame loc]
  (when loc
    (let [[d s] (nearest-shape w data raw-frame loc)]
      (when (and (number? d) (< d 20))
        [map->table (shape-properties s) [:shapes s]]))))
