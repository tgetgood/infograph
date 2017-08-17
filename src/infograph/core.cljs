(ns infograph.core
  (:require [reagent.core :as reagent]
            [re-frame.core :as re-frame]
            [infograph.events]
            [infograph.views :as views]
            [infograph.config :as config]))

(defn clear-window-resize-listener! []
  (set! (.-onresize js/window) nil))

(defn dev-setup []
  (when config/debug?
    (enable-console-print!)
    (println "dev mode")))

(defn mount-root []
  (re-frame/clear-subscription-cache!)
  (clear-window-resize-listener!)
  (reagent/render [views/main-panel]
                  (.getElementById js/document "app")))

(defn ^:export init []
  (re-frame/dispatch-sync [:infograph.events/initialize-db])
  (dev-setup)
  (mount-root))
