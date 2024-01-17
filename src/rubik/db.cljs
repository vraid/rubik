(ns rubik.db
  (:require [rubik.cube :as cube]
            [rubik.math.quaternion :as quaternion]
            [rubik.math.random :as random]
            [rubik.turns :as turns]))

(defn scrambles [max-time min-time turns-to-max-speed count]
  (let
   [half-count (* 0.5 count)
    base (Math/pow (/ min-time max-time)
                   (/ 1 turns-to-max-speed))
    time-at (fn [n] (* max-time (Math/pow base n)))]
    (reduce (fn [vec n]
              (let
               [time
                (cond
                  (< n (min turns-to-max-speed half-count)) (time-at n)
                  (>= n (max (- count turns-to-max-speed) half-count)) (time-at (- count n))
                  :else min-time)]
                (cons (turns/random-turn time (turns/turn-axis (and (seq vec) (first vec)))) vec)))
            []
            (range count))))

(def default-db
  (let
   [space-width 0.02
    space (Math/sin (/ Math/PI 12))
    vertices-per-side 4
    perspective (quaternion/from-axis-angle (random/random-axis) (random/random-angle))
    geometry (cube/geometry
              {:vertices vertices-per-side
               :spacing {:width space-width
                         :inner (- space space-width)
                         :outer (+ space space-width)}})
    initial-scrambles 40]
    {:geometry geometry
     :draw-data
     {:buffers (let
                [square-count (* 6 9)
                 vertex-count 3
                 buffer-size (* vertex-count square-count (+ 8 (* 16 vertices-per-side)))]
                 {:vertices (js/Float32Array. (* 3 buffer-size))
                  :colors (js/Float32Array. (* 4 buffer-size))})
      :shader nil
      :perspective perspective
      :square-rotation (fn [_] quaternion/identity)}
     :scale 5
     :perspective perspective
     :rotation {:disabled? false
                :paused? false
                :axis (random/random-axis)
                :speed 0.002}
     :time-per-frame 30
     :time-to-turn 600
     :past-turns []
     :turning false
     :control :none
     :initial-scramble-count initial-scrambles
     :initial-scramble (scrambles 600 120 8 initial-scrambles)
     :mouse-down false
     :mouse-event [:none [0 0]]
     :touch-start false
     :touch-event [:none [] []]}))
