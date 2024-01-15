(ns rubik.views
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [reagent.dom :as rdom]
            [thi.ng.geom.gl.core :as gl]
            [rubik.math.quaternion :as quaternion]
            [rubik.math.projection :as projection]
            [rubik.cube :as cube]
            [rubik.subs :as subs]
            [rubik.events :as events]
            [rubik.draw :as draw]))

(defn project-piece [proj]
  (fn [{:keys [center edges]}]
    {:center (proj center)
     :edges (mapv proj edges)}))

(defn project [projection {:keys [center pieces] :as square}]
  (let
   [projected-pieces (mapv (project-piece projection)
                           pieces)]
    (assoc square
           :center (projection center)
           :center-edges (let
                          [ls (mapcat (fn [piece]
                                        [(first (:edges piece))
                                         (:center piece)])
                                      projected-pieces)]
                           (conj (vec ls) (first ls)))
           :pieces projected-pieces)))

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
                                     (mapv (comp (partial project projection/stereographic)
                                                 (fn [square]
                                                   (cube/rotate-square
                                                    (quaternion/matrix-vector-product
                                                     (:rotation (:perspective props)))
                                                    square)))
                                           (:geometry props)))))]
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
