(ns infograph.views
  (:require [infograph.css :as css]
            [infograph.events :as events]
            [infograph.events.dom :as dom-events]
            [infograph.input :as input]
            [re-frame.core :as re-frame]
            [reagent.core :as reagent]))

(def resize-debouncer
  "Ad hoc debouncer for window resize events."
  (let [last-invoke (atom nil)
        go? (atom false)]
    (fn [canvas reset?]
      (if (true? reset?)
        (reset! last-invoke nil)
        (let [now (js/window.performance.now)]
          (if (< 500 (- now @last-invoke))
            (do
              (re-frame/dispatch [:infograph.events/resize-canvas canvas])
              (reset! last-invoke now)
              (reset! go? false))
            (when-not @go?
              (js/setTimeout  #(resize-debouncer canvas) (- now @last-invoke))
              (reset! go? true))))))))

(defn canvas-inner []
  (reagent/create-class
   {:component-did-mount  (fn [this]
                            (let [drawing (:hack (reagent/props this))
                                  elem (reagent/dom-node this)]
                              (set! (.-onresize js/window)
                                    (partial resize-debouncer elem))
                              (re-frame/dispatch
                               [(events/q :resize-canvas) elem])
                              (re-frame/dispatch
                               [(events/q :redraw-canvas) elem drawing])))
    :component-did-update (fn [this]
                            (let [drawing (:hack (reagent/props this))
                                  elem (reagent/dom-node this)]
                              (re-frame/dispatch
                               [(events/q :redraw-canvas) elem drawing])))
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

