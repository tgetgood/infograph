(ns infograph.views
  (:require [infograph.css :as css]
            [infograph.views.canvas :as canvas]
            [infograph.views.input-data :as input-data]
            [infograph.views.properties :as input]
            [re-frame.core :as re-frame]))

(defn side-panel [w data df canvas click drag]
  [:div
       [input/floating-property-window w data canvas drag]
       [:div
        (css/row [input-data/data-panel (:data data) df])
        (css/row [input/fixed-property-window w data canvas click])]])

;; REVIEW: Handling all the subscriptions here is getting to be a right mess.
(defn main-panel []
  (let [data       (re-frame/subscribe [:inst-data])
        window     (re-frame/subscribe [:window])
        drag       (re-frame/subscribe [:drag-position])
        click      (re-frame/subscribe [:shape-focus])
        data-focus (re-frame/subscribe [:data-focus])
        canvas-raw (re-frame/subscribe [:canvas-raw])
        canvas     (re-frame/subscribe [:canvas])]
    (fn []
      [css/row {:style {:height "100%"}}
       ^{:width 4}
       [side-panel @window @data @data-focus @canvas-raw @click @drag]
       ^{:width 8}
       [canvas/main @canvas]])))
