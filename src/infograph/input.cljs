(ns infograph.input
  (:require [cljs.reader :refer [read-string]]
            [infograph.css :as css]
            [infograph.locator :as locator]
            [infograph.shapes :as shapes]
            [re-frame.core :as re-frame]
            [reagent.core :as reagent]))

(def test-data
  {:name "demo-struct"
   :stuff [{:a 5} {:a 6}]
   :other '(false true 3 "sssasd" true)})

(defn nice-map []
  {:x (rand-int 500)
   :y (rand-int 1000)
   :length (rand-int 200)
   :label "I'm a someting"})

(def nice-vec
  (into [] (take 4 (repeatedly nice-map))))

(defprotocol ValueRender
  (render-value [this] [this query]))

(defn- receive-drop [query-path ev]
  (.stopPropagation ev)
  (re-frame/dispatch [:infograph.events.dom/reset-drag])
  (let [drop-path (read-string
                   (.getData (.-dataTransfer ev) "path"))]
    (re-frame/dispatch [:infograph.events/property-drop drop-path query-path])))

(defn drop-cbs [q]
  {:on-drag-over #(.preventDefault %)
         ;; TODO: Drag hover effects
         :on-drop (partial receive-drop q)})

(defn nested-values [v r]
  [:span (str v) " " r])

(defn value-dropper [v q]
  [:div (drop-cbs q)
   v])

(defn- pair-table [{:keys [x y]} q]
  [:table (drop-cbs q)
   [:tbody
    [:tr (drop-cbs (conj q :x))
     [:td [:span "x"]]
     [:td (render-value x)]]
    [:tr (drop-cbs (conj q :y))
     [:td [:span "y"]]
     [:td (render-value y)]]]])

;; REVIEW: Schemata are fundamentally different from projectables. Currently
;; that's dealt with by a variadic render function. What happens though when we
;; have projectables inside schemata?
(extend-protocol ValueRender
  default
  (render-value
    ([this]
     (str this))
    ([this path]
     (value-dropper (str this) path)))

  infograph.shapes.impl/Coordinate-2D
  (render-value [this q]
    (pair-table this q))

  infograph.shapes.impl/Vector-2D
  (render-value [this q]
    (pair-table this q))

  infograph.shapes.impl/Scalar
  (render-value [this q]
    (value-dropper (render-value (:v this)) (conj q :v)))

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

(defn floating-property-window []
  (let [drag-position (re-frame/subscribe [:drag-position])
        canvas        (re-frame/subscribe [:canvas-raw])
        data          (re-frame/subscribe [:inst-data])
        w             (re-frame/subscribe [:window])]
    (fn []
      (when @drag-position
        (let [[x y :as loc] @drag-position
              [d s]         (nearest-shape @w @data @canvas loc)
              [ox oy]       (:offset @w)]
          (if (and d (< d 20))
            [:div {:style {:position        "absolute"
                           :backgroundColor "rgba(255,255,255,0.8)"
                           :top             (+ y 20)
                           :left            (+ ox x)}}
             [map->table (shape-properties s) [:shapes s]]]
            [:div {:style {:display :none}}]))))))

(defn fixed-property-window [w data raw-frame loc]
  (when loc
    (let [[d s] (nearest-shape w data raw-frame loc)]
      (when (and (number? d) (< d 20))
        [map->table (shape-properties s) [:shapes s]]))))

;; Colours stolen from klipse:
;;
;; brackets #997
;; string #a11
;; keyword #219
;; number #164

(defprotocol IRender
  (render [this focus path] "Produce hiccup from renderable data."))

(extend-protocol IRender
  nil
  (render [_ _ _]
    (.error js/console "Attempting to render nil input element.")))

(defn drag-start [p ev]
  (.setData (.-dataTransfer ev) "path" p))

(defn string-component
  [x _ p]
  (fn []
    [:span {:style {:color "#a11"}
            :on-touch-move #(.preventDefault %)
            :draggable true
            :on-drag-start (partial drag-start p)}
     x]))

(defn number-component
  [x {:keys [open?]} p]
  [:div
   [:span {:style {:color "#164"}
           :on-touch-move #(.preventDefault %)
           :draggable true
           :on-drag-start (partial drag-start p)
           :on-click #(re-frame/dispatch [:infograph.events/toggle-data p])}
    (str x)]
   (when open?
     [:input {:style {:float "right"
                      :margin-bottom "2px"}
              :defaultValue x
              :min 0
              :max 2000
              :on-click #(.stopPropagation %)
              :on-change #(re-frame/dispatch
                           [:update-data-value p
                            (int (.-value (.-target %)))])
              :type :range}])])

(defn boolean-component
  [x _ p]
  [:span {:style {:color "#219"}
          :draggable true}
   (str x)])

(defn vector-component
  [v {:keys [open? children] :as focus} query-path]
  ;; TODO: Keys. What am I going to use, UUIDs? The input data is more or less
  ;; static at the moment, so this hack isn't so bad...'
  [:div {:style {:border       "solid"
                 :border-color "#997"
                 :border-width "1px"}}
   [:span {:on-click
           #(re-frame/dispatch [:infograph.events/toggle-data query-path])}
    (if open? "collapse [" "expand []")]
   (when open?
     (into
      [:div]
      (map-indexed (fn [i x]
                     [css/row
                      [render x (get children i) (conj query-path i)]])
                   v)))])


#_(defn set-component
  [x]
  (vector-component x ))

(defn map-component
  [m {:keys [open? children] :as f} query-path]
  [:div {:style {:border       "solid"
                 :border-color "#a1f"
                 :border-width "1px"}}
   [:span {:on-click
           #(re-frame/dispatch [:infograph.events/toggle-data query-path])}
    (if open? "collapse {" "expand {}")]
   (when open?
     (map (fn [[k e]]
            ^{:key k}
            [css/row
             [:span {:style {:color "#a1f"}} k]
             [render e (get children k) (conj query-path k)]])
       m))])

(def render-map
  ;; TODO: More types...
  {js/String                    string-component
   js/Number                    number-component
   js/Boolean                   boolean-component
   ;; keyword
   ;; symbol
   cljs.core/PersistentArrayMap map-component
   cljs.core/PersistentHashMap  map-component
   cljs.core/PersistentTreeMap  map-component
   ;; cljs.core/PersistentHashSet  set-component
   ;; cljs.core/PersistentTreeSet  set-component
   cljs.core/List               vector-component
   cljs.core/PersistentVector   vector-component})

(doseq [[t c] render-map]
  (extend-type t
    IRender
    (render [x f p] (c x f p))))

(defn data-panel [data focus]
  [render data focus []])

(defn side-panel [w data df canvas click]
  [:div
       [floating-property-window]
       [:div
        (css/row [data-panel (:data data) df])
        (css/row [fixed-property-window w data canvas click])]])

(defn wired-data []
  (let [data       (re-frame/subscribe [:inst-data])
        window     (re-frame/subscribe [:window])
        click      (re-frame/subscribe [:shape-focus])
        data-focus (re-frame/subscribe [:data-focus])
        canvas     (re-frame/subscribe [:canvas-raw])]
    (fn []
      [side-panel @window @data @data-focus @canvas @click])))
