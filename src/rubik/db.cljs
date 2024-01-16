(ns rubik.db
  (:require [rubik.cube :as cube]
            [rubik.math.quaternion :as quaternion]
            [rubik.math.random :as random]))

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
                         :outer (+ space space-width)}})]
    {:geometry geometry
     :draw-data
     {:buffers (let
                [square-count (* 6 9)
                 vertex-count 3
                 buffer-size (* vertex-count square-count (+ 8 (* 16 vertices-per-side)))]
                 {:vertices (js/Float32Array. (* 3 buffer-size))
                  :colors (js/Float32Array. (* 4 buffer-size))})
      :shader nil
      :perspective perspective}
     :scale 5
     :perspective perspective
     :rotation {:paused? false
                :axis (random/random-axis)
                :speed 0.002}
     :time-per-frame 30
     :mouse-down false
     :mouse-event [:none [0 0]]}))
