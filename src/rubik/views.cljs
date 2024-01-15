(ns rubik.views
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [reagent.dom :as rdom]
            [rubik.subs :as subs]
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

(defn orthographic-projection [a]
  (let
   [[x y _] a]
    #js [x y 0]))

(defn canvas-inner []
  (let [draw (fn [canvas]
               (let
                [props (reagent/props canvas)]
                 (draw/draw-canvas (rdom/dom-node canvas)
                                   (:buffers props)
                                   (mapv (partial project orthographic-projection)
                                         (:geometry props)))))]
    (reagent/create-class
     {:reagent-render (fn []
                        [:canvas {:width 1000
                                  :height 1000
                                  :style {:display "block"}}])
      :component-did-mount draw
      :component-did-update draw
      :display-name "gl-canvas"})))

(defn canvas-outer []
  (let [data (re-frame/subscribe [::subs/data])]
    [canvas-inner @data]))

(defn main-panel []
  (let [name (re-frame/subscribe [::subs/name])]
    [:div {:style {:width "100vw" :height "100vh" :max-width "100%"}}
     [canvas-outer]
     [:h1
      "Hello from " @name]]))
