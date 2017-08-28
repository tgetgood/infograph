(ns infograph.views.canvas
  (:require [infograph.css :as css]
            [infograph.events :as events]
            [infograph.events.dom :as dom-events]
            [re-frame.core :as re-frame]
            [reagent.core :as reagent]))


(defn canvas-inner []
  (reagent/create-class
   {:component-did-mount  (fn [this]
                            (let [drawing (:hack (reagent/props this))
                                  elem (reagent/dom-node this)]
                              (set! (.-onresize js/window)
                                    #(events/resize-canvas-debounced elem))
                              (re-frame/dispatch [::events/resize-canvas elem])
                              (re-frame/dispatch
                               [::events/redraw-canvas elem drawing])))
    :component-did-update (fn [this]
                            (let [drawing (:hack (reagent/props this))
                                  elem (reagent/dom-node this)]
                              (re-frame/dispatch
                               [::events/redraw-canvas elem drawing])))
    :reagent-render       (let [mode (re-frame/subscribe [:input-mode])]
                            (fn []
                              [:canvas
                               (assoc dom-events/canvas-event-handlers
                                      :id "the-canvas")]))}))

(defn canvas-panel [drawing]
  [:div {:id "canvas-container"
         :on-drag-over #(.preventDefault %)
         :on-drag-enter #(.preventDefault %)
         :style {:width "100%"
                 :height "100%"
                 :overflow "hidden"}}
   ;; HACK: Custom types don't get picked up by reagent/props. They can be taken
   ;; out at a lower level, but I don't see that as any better.
   [canvas-inner {:hack drawing}]])

(defn input-mode [type]
  {:on-click
   (fn [_]
     (re-frame/dispatch [:set-input-mode type]))})

(defn widgets-panel []
  [css/row
   [css/button (input-mode :grab) "grab"]
   [css/button (input-mode :line) "line"]
   [css/button (input-mode :rectangle) "rectangle"]
   [css/button (input-mode :circle) "circle"]])

(defn main [drawing]
  [:div {:style {:height "100%"}}
   [widgets-panel]
   [canvas-panel drawing]])
