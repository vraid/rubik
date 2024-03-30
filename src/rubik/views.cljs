(ns rubik.views
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [reagent.dom :as rdom]
            [thi.ng.geom.gl.core :as gl]
            [rubik.subs :as subs]
            [rubik.events :as events]
            [rubik.input :as input]
            [rubik.transform :as transform]
            [rubik.graphics :as graphics]))

(defn canvas-inner []
  (let [mount (fn [canvas]
                (let
                 [shader (-> canvas rdom/dom-node gl/gl-context graphics/make-shader)]
                  (re-frame/dispatch [::events/set-shader shader])))
        update (fn [canvas]
                 (let
                  [props (reagent/props canvas)
                   gl (gl/gl-context (rdom/dom-node canvas))]
                   (->> (transform/transform-data (:geometry props) (:perspective props) (:square-rotation props))
                        (graphics/to-model (:buffers props))
                        (graphics/draw-canvas gl (:shader props) (graphics/projection (:scale props))))))]
    (reagent/create-class
     {:reagent-render (fn []
                        [:canvas {:width 1000
                                  :height 1000
                                  :on-mouse-down input/mouse-down
                                  :on-touch-start input/touch-start
                                  :on-touch-end input/touch-end
                                  :on-touch-move input/touch-move
                                  :on-touch-cancel input/touch-cancel
                                  :style {:display "block"}}])
      :component-did-mount mount
      :component-did-update update
      :display-name "gl-canvas"})))

(defn canvas-outer []
  (let [data (re-frame/subscribe [::subs/graphics])]
    [canvas-inner @data]))

(def button-style
  {:padding "8px 8px"
   :margin "4px 4px"})

(defn set-control [a]
  (fn [] (re-frame/dispatch [::events/control a])))

(defn main-panel []
  (let
   [past-turns (re-frame/subscribe [::subs/past-turns])
    control (re-frame/subscribe [::subs/control])
    initial-scramble-count (re-frame/subscribe [::subs/initial-scramble-count])
    initial-scramble (re-frame/subscribe [::subs/initial-scramble])
    still-scrambling? (seq @initial-scramble)
    rotation-disabled? (re-frame/subscribe [::subs/rotation-disabled?])]
    [:div {:on-mouse-up input/mouse-up
           :on-mouse-move input/mouse-move
           :style {:width "100vw" :height "100vh" :max-width "100%"}}
     [canvas-outer]
     [:h3 (if still-scrambling?
            (let
             [total @initial-scramble-count
              current (count @initial-scramble)]
              (str "Scrambling " (* 10 (Math/floor (* 10 (/ (- total current) total)))) "%"))
            "")]
     (when (not still-scrambling?)
       [:div
        [:h3 "Welcome to the inside of a Rubik's cube. Click and drag to rotate, or ctrl + drag to change perspective"]
        [:h3 "On touch screens, use one finger to rotate, or two to change perspective"]
        [:h3 "Good luck!"]])
     [:button
      {:style button-style
       :disabled (some #{@control} [:none :initial])
       :on-click (fn [_] (re-frame/dispatch [::events/reset]))}
      "Reset to solved state"]
     [:button
      {:style button-style
       :disabled (or (some #{@control} [:initial :rewind]) (empty? @past-turns))
       :on-click (set-control :rewind)}
      "Rewind"]
     [:button
      {:style button-style
       :disabled (some #{@control} [:none :initial :scramble])
       :on-click (set-control :scramble)}
      "Scramble"]
     [:button
      {:style button-style
       :disabled (not (some #{@control} [:scramble :rewind]))
       :on-click (set-control :manual)}
      "Halt"]
     [:button
      {:style button-style
       :on-click (fn [_] (re-frame/dispatch [::events/disable-rotation (not @rotation-disabled?)]))}
      "Toggle rotation"]
     [:h3]
     [:div "Check it out on "
      [:a {:href "https://github.com/vraid/rubik/"}
       "Github"]]
     [:h3]]))
