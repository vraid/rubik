(ns rubik.views
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [reagent.dom :as rdom]
            [thi.ng.geom.gl.core :as gl]
            [rubik.subs :as subs]
            [rubik.events :as events]
            [rubik.input :as input]
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
                                     (:scale props)
                                     (:buffers props)
                                     (transform/transform-data
                                      (:geometry props)
                                      (:perspective props)
                                      (:square-rotation props)))))]
    (reagent/create-class
     {:reagent-render (fn []
                        [:canvas {:width 1000
                                  :height 1000
                                  :on-mouse-down input/mouse-down
                                  :on-touch-start input/touch-start
                                  :style {:display "block"}}])
      :component-did-mount mount
      :component-did-update update
      :display-name "gl-canvas"})))

(defn canvas-outer []
  (let [data (re-frame/subscribe [::subs/draw-data])]
    [canvas-inner @data]))

(defn main-panel []
  [:div {:on-mouse-up input/mouse-up
         :on-mouse-move input/mouse-move
         :on-touch-end input/touch-end
         :on-touch-move input/touch-move
         :on-touch-cancel input/touch-cancel
         :style {:width "100vw" :height "100vh" :max-width "100%"}}
   [canvas-outer]
   [:h1 "Welcome to Rubik"]])
