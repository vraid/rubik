(ns rubik.core
  (:require [reagent.dom :as rdom]
            [re-frame.core :as re-frame]
            [rubik.events :as events]
            [rubik.views :as views]
            [rubik.config :as config]))

(defn dev-setup []
  (when config/debug?
    (println "dev mode")))

(defn ^:dev/after-load mount-root []
  (re-frame/clear-subscription-cache!)
  (let [root-el (.getElementById js/document "app")]
    (rdom/unmount-component-at-node root-el)
    (rdom/render [views/main-panel] root-el)))

(defn dispatch-tick-event []
  (re-frame/dispatch [::events/tick]))

(defn dispatch-start [ms]
  (re-frame/dispatch [::events/start-in ms]))

(defn init []
  (re-frame/dispatch-sync [::events/initialize-db])
  (dev-setup)
  (mount-root)
  (dispatch-tick-event)
  (dispatch-start 1200))
