(ns rubik.db
  (:require [rubik.cube :as cube]))

(def default-db
  (let
   [space-width 0.02
    space (Math/sin (/ Math/PI 12))
    vertices-per-side 4
    geometry (cube/geometry
              {:vertices vertices-per-side
               :spacing {:width space-width
                         :inner (- space space-width)
                         :outer (+ space space-width)}})]
    {:geometry geometry
     :buffers (let
               [square-count 9
                vertex-count 3
                buffer-size (* vertex-count square-count (+ 8 (* 16 vertices-per-side)))]
                {:vertices (js/Float32Array. (* 3 buffer-size))
                 :colors (js/Float32Array. (* 4 buffer-size))})}))
