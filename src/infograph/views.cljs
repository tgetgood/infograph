(ns infograph.views
  (:require [infograph.css :as css]
            [infograph.events :as events]
            [infograph.events.dom :as dom-events]
            [infograph.input :as input]
            [re-frame.core :as re-frame]
            [reagent.core :as reagent]))

(defn canvas-inner []
  (reagent/create-class
   {:component-did-mount  (fn [this]
                            (re-frame/dispatch [(events/q :resize-canvas)])
                            (let [drawing (reagent/props this)]
                              (re-frame/dispatch
                               [(events/q :redraw-canvas) drawing])))
    :component-did-update (fn [this]
                            (let [drawing (reagent/props this)]
                              (re-frame/dispatch
                               [(events/q :redraw-canvas) drawing])))
    :reagent-render       (let [mode (re-frame/subscribe [:input-mode])]
                            (fn []
                              [:canvas
                               ;; REVIEW: I don't like the globalness of using
                               ;; the element ID to access the canvas throughout
                               ;; the app. What do we do if we suddenly want
                               ;; multiple canvases? What's a better way to do
                               ;; this? Does it matter in this case?
                               (assoc dom-events/canvas-event-handlers
                                      :id "the-canvas")]))}))

(defn canvas-panel [drawing]
  [:div {:id "canvas-container"
         :style {:width "100%"
                 :height "100%"
                 :overflow "hidden"}}
   [canvas-inner drawing]])

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

;; REVIEW: This convention of having pure components and wired components serves
;; a good purpose: it encourages components to be pure and stateless, which
;; makes it easier to develop and document them in devcards. It's also serving
;; to let me push the subscriptions further and further up the component tree
;; which is interesting.
;;
;; The downside is a hacky looking naming convention that I have to put extra
;; effort into keeping consistent.
(defn wired-panel []
  (let [drawing (re-frame/subscribe [:canvas])]
    (fn []
      [:div {:style {:height "100%"}}
       [widgets-panel]
       [canvas-panel @drawing]])))

(defn main-panel []
  [css/row {:style {:height "100%"}}
   ^{:width 3} [input/wired-data]
   ^{:width 9} [wired-panel]])

