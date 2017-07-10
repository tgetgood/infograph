(ns infograph.views
  (:require [infograph.canvas :as canvas]
            [infograph.css :as css]
            [infograph.input :as input]
            [re-frame.core :as re-frame]
            [reagent.core :as reagent]))

(defn event [name]
  (keyword :infograph.events name))

(defn ev->handler [ev]
  (keyword (str "on-" (name ev))))

(def events
  [:wheel
   :click
   :mouse-down
   :mouse-move
   :mouse-up
   :touch-start
   :touch-move
   :touch-end
   ;; Looks like we're going to have to deal with hover from scratch now as well
   ])

(defn canvas-event-handlers [mode]
  (into {}
        (map (fn [x]
               [(ev->handler x) (partial canvas/handler x mode)])
          events)))

(defn- manual-props
  "Why??"
  ;; HACK: reagent has a fn for this. Why isn't it working?
  [this]
  (-> this (aget "props") (aget "argv") second))

(defn canvas-inner []
  (reagent/create-class
   {:component-did-mount  (fn [this]
                            (re-frame/dispatch [(event :resize-canvas)])
                            (let [drawing (manual-props this)]
                              (re-frame/dispatch [(event :redraw-canvas) drawing])))
    :component-did-update (fn [this]
                            (let [drawing (manual-props this)]
                              (re-frame/dispatch [(event :redraw-canvas) drawing])))
    :reagent-render       (let [mode (re-frame/subscribe [:input-mode])]
                            (fn []
                              [:canvas
                               ;; REVIEW: I don't like the globalness of using
                               ;; the element ID to access the canvas throughout
                               ;; the app. What do we do if we suddenly want
                               ;; multiple canvases? What's a better way to do
                               ;; this?
                               (assoc (canvas-event-handlers @mode)
                                      :id "the-canvas")]))}))

(defn canvas-panel [drawing]
  [canvas-inner drawing])

(defn input-mode [type]
  {:on-click
   (fn [_]
     (re-frame/dispatch [:set-input-mode type]))})

(defn widgets-panel []
  [css/row
   [css/button (input-mode :line) "line"]
   [css/button (input-mode :polyline) "polyline"]
   [css/button (input-mode :rectangle) "rectangle"]
   [css/button (input-mode :polygon) "polygon"]
   [css/button (input-mode :circle) "circle"]
   [css/button (input-mode :ellipse) "ellipse"]])

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
  (let [drawing (re-frame/subscribe [:canvas])]
    (fn []
      [:div
       [widgets-panel]
       [canvas-panel @drawing]])))

(defn main-panel []
  [css/row
   [input/wired-data]
   [wired-panel]])

