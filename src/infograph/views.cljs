(ns infograph.views
  (:require [infograph.css :as css]
            [infograph.input :as input]
            [re-frame.core :as re-frame]
            [reagent.core :as reagent]))

(defn event [name]
  (keyword :infograph.views name))

(defn canvas-inner []
  (reagent/create-class
   {:component-did-mount  (fn [this]
                            (re-frame/dispatch [(event :resize-canvas)])
                            (let [drawing (reagent/props this)]
                              (re-frame/dispatch [(event :redraw-canvas) drawing])))
    :component-did-update (fn [this]
                            (let [drawing (reagent/props this)]
                              (re-frame/dispatch [(event :redraw-canvas) drawing])))
    :reagent-render       (fn []
                            [:canvas
                             {:id "the-canvas"}
                             #_(assoc events/canvas-event-map
                                    :id "the-canvas")])}))

(defn canvas-panel [drawing]
  [canvas-inner drawing])

;; HACK: This convention of having pure components and wired components serves a
;; good purpose: it encourages components to be pure and stateless, which makes
;; it easier to develop and document them in devcards. It's also serving to let
;; me push the subscriptions further and further up the component tree which is
;; interesting.
;;
;; The downside is a hacky looking naming convention that I have to put extra
;; effort into keeping consistent.
;; TODO: Keep an eye out for better ways to accomplish this.

(defn wired-panel []
  (let [drawing (re-frame/subscribe [:current-shape-data])]
    (fn []
      [canvas-panel @drawing])))

(defn main-panel []
  [css/row
   [input/data-panel input/nice-vec]
   [canvas-panel {}]])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; Effects
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn canvas []
  ;;FIXME: This won't do very shortly
  (.getElementById js/document "the-canvas"))

(defn get-ctx []
  (when-let [canvas (canvas)]
    (.getContext canvas "2d")))

(defn width [] (quot (.-innerWidth js/window) 2))
(defn height [] (.-innerHeight js/window))

(defn set-canvas-size! [canvas]
  (set! (.-width canvas) (- (width) 10))
  (set! (.-height canvas) (- (height) 10)))

(defn clear! [ctx]
  (.clearRect ctx 0 0 (width) (height)))

(re-frame/reg-fx
 ::redraw-canvas!
 (fn [drawing]
   (when drawing
     (when-let [ctx (get-ctx)]
       ))))

(re-frame/reg-fx
 ::resize-canvas!
 (fn [_]
   (set-canvas-size! (canvas))))

(re-frame/reg-event-fx
 ::resize-canvas
 (fn [_ _]
   {::resize-canvas! true}))

(re-frame/reg-event-fx
 ::redraw-canvas
 (fn [{[_ d] :event :as a}]
   {::redraw-canvas! d}))
