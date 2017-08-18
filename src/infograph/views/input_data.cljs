(ns infograph.views.input-data
  (:require [infograph.css :as css]
            [re-frame.core :as re-frame]))

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
