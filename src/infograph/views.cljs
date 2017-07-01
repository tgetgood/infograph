(ns infograph.views
  (:require [re-frame.core :as re-frame]))

(defn event [name]
  (keyword :infograph.events name))

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
                             (assoc events/canvas-event-map
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
  (let [name (re-frame/subscribe [:name])]
    (fn []
      [:div "Hello from " @name])))
