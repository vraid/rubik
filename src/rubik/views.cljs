(ns rubik.views
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [reagent.dom :as rdom]
            [thi.ng.geom.gl.core :as gl]
            [rubik.subs :as subs]
            [rubik.events :as events]
            [rubik.transform :as transform]
            [rubik.draw :as draw]))

(defn canvas-inner []
  (let [mount (fn [canvas]
                (let
                 [shader (-> canvas rdom/dom-node gl/gl-context draw/make-shader)]
                  (re-frame/dispatch [::events/set-shader shader])))
        update (fn [canvas]
                 (let
                  [props (reagent/props canvas)]
                   (draw/draw-canvas (rdom/dom-node canvas)
                                     (:shader props)
                                     (:buffers props)
                                     (transform/transform-data
                                      (:geometry props)
                                      (:rotation (:perspective props))))))]
    (reagent/create-class
     {:reagent-render (fn []
                        [:canvas {:width 1000
                                  :height 1000
                                  :style {:display "block"}}])
      :component-did-mount mount
      :component-did-update update
      :display-name "gl-canvas"})))

(defn canvas-outer []
  (let [data (re-frame/subscribe [::subs/data])]
    [canvas-inner @data]))

(defn main-panel []
  [:div {:style {:width "100vw" :height "100vh" :max-width "100%"}}
   [canvas-outer]
   [:h1 "Welcome to Rubik"]])
