(ns infograph.input
  (:require [infograph.css :as css]))

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

;; colour stolen from klipse
;;
;; brackets #997
;; string #a11
;; keyword #219
;; number #164

(defprotocol IRender
  (render [_] "Produce hiccup from renderable data."))

(defn string-component
  [x]
  [:span {:style {:color "#a11"}
          :draggable true}
   x])

(defn number-component
  [x]
  [:span {:style {:color "#164"}
          :draggable true}
   (str x)])

(defn boolean-component
  [x]
  [:span {:style {:color "#219"}
          :draggable true}
   (str x)])

(defn vector-component
  [x]
  ;; TODO: Keys. What am I going to use, UUIDs? The input data is more or less
  ;; static at the moment, so this hack isn't so bad...'
  `[:div 
    [:span {:style {:color "#997"}} "["]
    ~@(map (fn [x] (render x)) x)
    [:span {:style {:color "#997"}} "]"]])

(defn set-component
  [x]
  (vector-component x))

(defn map-entry-component
  [[k v]]
  [css/row
   [:span {:style {:color "#a1f"}} k]
   (render v)])

(defn map-component
  [x]
  [:span
   (map (fn [[k v :as x]]
          ^{:key k} [map-entry-component x])
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
   cljs.core/PersistentHashSet  set-component
   cljs.core/PersistentTreeSet  set-component
   cljs.core/List               vector-component
   cljs.core/PersistentVector   vector-component})

(doseq [[t c] render-map]
  (extend-type t
    IRender
    (render [x] (c x))))

(defn data-panel [data]
  [:div
   (render data)])
