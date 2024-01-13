(ns rubik.views
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [reagent.dom :as rdom]
            [thi.ng.geom.gl.core :as gl]
            [rubik.subs :as subs]))

(defn draw-canvas [canvas]
  (let [gl (gl/gl-context canvas)]
    (gl/clear-color-and-depth-buffer gl 0 0 0 1 1)))

(defn canvas []
  (let [draw (fn [canvas]
               (draw-canvas (rdom/dom-node canvas)))]
    (reagent/create-class
     {:reagent-render (fn []
                        [:canvas {:width 1000
                                  :height 1000
                                  :style {:display "block"}}])
      :component-did-mount draw
      :component-did-update draw
      :display-name "gl-canvas"})))

(defn main-panel []
  (let [name (re-frame/subscribe [::subs/name])]
    [:div {:style {:width "100vw" :height "100vh" :max-width "100%"}}
     [canvas]
     [:h1
      "Hello from " @name]]))
