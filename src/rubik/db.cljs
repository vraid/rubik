(ns rubik.db
  (:require [rubik.cube :as cube]))

(def default-db
  (let
   [space-width 0.02
    space (Math/sin (/ Math/PI 12))
    vertices-per-side 4
    geometry [(cube/center-square
               {:vertices vertices-per-side
                :space-inner (- space space-width)})]]
    {:name "re-frame"
     :geometry geometry
     :buffers (let
               [vertex-count 3
                buffer-size (* vertex-count (+ 8 (* 16 vertices-per-side)))]
                {:vertices (js/Float32Array. (* 3 buffer-size))
                 :colors (js/Float32Array. (* 4 buffer-size))})}))