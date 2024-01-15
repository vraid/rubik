(ns rubik.cube
  (:require [rubik.math.vector :as vector]
            [rubik.math.quaternion :as quaternion]))

(defn square [a] (* a a))

(defn stepwise-vertices [vertices axis angle vector]
  (mapv (fn [n]
          (quaternion/vector-product
           (quaternion/from-axis-angle axis (* (/ n (dec (* 2 vertices))) angle))
           vector))
        (range (* 2 vertices))))

(defn rotate-piece [rotated {:keys [center edges]}]
  {:center (rotated center)
   :edges (mapv rotated edges)})

(defn create-piece [vertices center corner [a-axis a-angle] [b-axis b-angle]]
  (let
   [stepwise (partial stepwise-vertices vertices)]
    {:center (vector/normal (vector/sum corner (vector/scale-by 0.6 center)))
     :edges (concat (reverse (rest (stepwise a-axis (- a-angle) corner)))
                    (stepwise b-axis b-angle corner))}))

(defn center-square [settings]
  (let
   [{vertices :vertices
     space-inner :space-inner} settings
    base-length (Math/sqrt (- 1 (square space-inner)))
    base-xy space-inner
    base-z (Math/sqrt (- (square base-length) (square base-xy)))
    angle (Math/atan2 base-xy base-z)
    corner [base-xy base-xy (- base-z)]
    rotations (map (comp (partial quaternion/from-axis-angle [0 0 1])
                         (partial * 0.5 Math/PI))
                   (range 4))
    center [0 0 -1]
    piece (create-piece vertices center corner [[1 0 0] angle] [[0 1 0] angle])]
    {:coordinates [0 0 -1]
     :normal [0 0 -1]
     :center center
     :pieces
     (mapv (fn [rotation]
             (rotate-piece (partial quaternion/vector-product rotation) piece))
           rotations)}))
