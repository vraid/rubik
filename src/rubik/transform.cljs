(ns rubik.transform
  (:require [rubik.math.quaternion :as quaternion]
            [rubik.math.projection :as projection]
            [rubik.cube :as cube]))

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

(defn transform-data [geometry rotation]
  (mapv (comp (partial project projection/stereographic)
              (partial cube/rotate-square
                       (quaternion/matrix-vector-product
                        rotation)))
        geometry))
