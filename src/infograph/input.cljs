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
   :length (rand-int 20 200)
   :label "I'm a something"})

(def nice-vec
  (into [] (take 4 (repeatedly nice-map))))

(defprotocol ValueRender
  (render-value [this] [this query]))

(defn- receive-drop [query-path ev]
  (.stopPropagation ev)
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
  (render-value [{:keys [x y]} q]
    [:table (drop-cbs q)
     [:tbody
      [:tr (drop-cbs (conj q :x))
       [:td [:span "x"]]
       ;; FIXME: Passing nil is definitely a mistake
       [:td (render-value x)]]
      [:tr (drop-cbs (conj q :y))
       [:td [:span "y"]]
       [:td (render-value y)]]]])

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
  (select-keys s [:p :q]))

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

(defn property-window []
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
             ;;FIXME: We're going to have to pass in the uninstantiated shape
             ;;since that's the shape in the main app db that we're querying
             ;;against.
             [map->table (shape-properties s) [:shapes s]]]
            [:div {:style {:display :none}}]))))))

;; Colours stolen from klipse:
;;
;; brackets #997
;; string #a11
;; keyword #219
;; number #164

(defprotocol IRender
  ;; FIXME: Worst. Name. Ever.
  ;; But seriously you have to remember to invoke it as hiccup, not a function,
  ;; and I keep tripping on that.
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
  [x _ p]
  (let [open? (reagent/atom false)]
    (fn [x p]
      [:div
       [:span {:style {:color "#164"}
               :on-touch-move #(.preventDefault %)
               :draggable true
               :on-drag-start (partial drag-start p)
               :on-click #(swap! open? not)}
        (str x)]
       (when @open?
         [:input {:style {:float "right"
                          :margin-bottom "2px"}
                  :defaultValue x
                  :min 0
                  :max 2000
                  :on-click #(.stopPropagation %)
                  :on-change #(re-frame/dispatch
                               [:update-data-value p
                                (int (.-value (.-target %)))])
                  :type :range}])])))

(defn boolean-component
  [x _ p]
  [:span {:style {:color "#219"}
          :draggable true}
   (str x)])

(defn vector-component
  [x focus p]
  ;; TODO: Keys. What am I going to use, UUIDs? The input data is more or less
  ;; static at the moment, so this hack isn't so bad...'
  [:div {:style {:border "solid"
                 :border-color "#997"
                 :border-width "1px"}}
   [:span {:on-click #(swap! open? not)}
    (if @open? "collapse" "expand")]
   (cond
     @open?    `[:div
                 ~@(map-indexed (fn [i x] [render x (conj p i)]) x)]
     @selected [render (get x @selected) (conj p @selected)]
     ;; REVIEW: Explicit nil is better than fall through right? Or is it
     ;; just confusing?
     :else     nil)])

#_(defn set-component
  [x]
  (vector-component x ))

(defn map-entry-component
  [[k v] p]
  [css/row
   [:span {:style {:color "#a1f"}} k]
   [render v (conj p k)]])

(defn map-component
  [x _ p]
  [:span
   (map (fn [[k v :as x]]
          ^{:key k} [map-entry-component x p])
     x)])

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
    (render [x p] (c x p))))

(defn data-panel [data focus]
  [render data focus []])

(defn wired-data []
  (let [data  (re-frame/subscribe [:data])
        focus (re-frame/subscribe [:data-focus])]
    (fn []
      [:div
       [property-window]
       [data-panel @data @focus]])))
