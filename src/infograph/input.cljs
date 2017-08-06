(ns infograph.input
  (:require [infograph.css :as css]
            [infograph.locator :as locator]
            [infograph.window :as window]
            [re-frame.core :as re-frame]
            [reagent.core :as reagent]))

(def test-data
  {:name "demo-struct"
   :stuff [{:a 5} {:a 6}]
   :other '(false true 3 "sssasd" true)})

(def nice-map
  {:x 234
   :y 1093
   :length 12
   :label "I'm a something"})

(def nice-vec
  (into [] (take 4 (repeat nice-map))))

(defn- table-row [[k v]]
  [:tr
   [:td [:span k]]
   [:td
    [:div {:on-drag-over #(.preventDefault %)
           :on-drop #(js/console.log "dropped table")}
     (str v)]]])

(defn map->table [m]
  `[:table
    [:thead
     [:tr
      [:th [:span "Property"]]
      [:th [:span "Value"]]]]
    [:tbody
     ~@(map table-row m)]])

;; TODO: Unified shape classification system.
(defn- classify [s] (:type s))

(defmulti shape-properties classify)

(defmethod shape-properties :circle
  [s]
  (select-keys s [:c :r]))

(defmethod shape-properties :line
  [s]
  (select-keys s [:p :q]))

(defn properties [s]
  [map->table (shape-properties s)])

(defn nearest-shape [{:keys [shapes]} loc]
  (->> shapes
       (map (fn [s] [(locator/dist s loc) s]))
       (sort-by first)
       first))

(defn property-window []
  (let [drag-position (re-frame/subscribe [:drag-position])
        canvas (re-frame/subscribe [:canvas])
        w (re-frame/subscribe [:window])]
    (fn []
      (when @drag-position
        (let [[x y :as loc] @drag-position
              [d s] (nearest-shape @canvas loc)
              [ox oy] (:offset @w)]
          (if (and d (< d 10))
            [:div {:style {:position "absolute"
                           :backgroundColor "rgba(255,255,255,0.8)"
                           :top (+ y 20)
                           :left (+ ox x)}}
             [properties s]]
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
  (render [this path] "Produce hiccup from renderable data."))

(extend-protocol IRender
  nil
  (render [_ _]
    (.error js/console "Attempting to render nil input element.")))

(defn drag-start [p ev]
  (.setData (.-dataTransfer ev) "path" p))

(defn string-component
  [x p]
  (fn []
    [:span {:style {:color "#a11"}
            :on-touch-move #(do (.preventDefault n%))
            :draggable true}
     x]))

(defn number-component
  [x p]
  (let [open? (reagent/atom false)]
    (fn [x p]
      [:div
       [:span {:style {:color "#164"}
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
  [x p]
  [:span {:style {:color "#219"}
          :draggable true}
   (str x)])

(defn vector-component
  [x p]
  ;; TODO: Keys. What am I going to use, UUIDs? The input data is more or less
  ;; static at the moment, so this hack isn't so bad...'
  `[:div
    [:span {:style {:color "#997"}} "["]
    ~@(map-indexed (fn [i x] [render x (conj p i)]) x)
    [:span {:style {:color "#997"}} "]"]])

#_(defn set-component
  [x]
  (vector-component x ))

(defn map-entry-component
  [[k v] p]
  [css/row
   [:span {:style {:color "#a1f"}} k]
   [render v (conj p k)]])

(defn map-component
    [x p]
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

(defn data-panel [data]
  [render data []])

(defn wired-data []
  (let [data (re-frame/subscribe [:data])]
    (fn []
      [:div
       [property-window]
       [data-panel @data]])))
