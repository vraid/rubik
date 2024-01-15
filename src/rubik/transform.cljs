(ns rubik.transform
  (:require [rubik.math.vector :as vector]
            [rubik.math.quaternion :as quaternion]
            [rubik.math.projection :as projection]
            [rubik.polygon :as polygon]
            [rubik.cube :as cube]))

(defn project-piece [proj]
  (fn [{:keys [center edges]}]
    {:center (proj center)
     :edges (mapv proj edges)}))

(defn project [center? projection {:keys [center pieces max-edge-distance] :as a}]
  (let
   [projected-center (projection center)
    pole-distance (partial vector/squared-distance [0 0 1])
    center-distance (pole-distance center)
    close? (< center-distance max-edge-distance)
    projected-pieces (mapv (project-piece projection)
                           pieces)
    centered? (and center?
                   close?
                   ((polygon/in-polygon? [0 0])
                    (polygon/to-polygon (mapcat (comp rest :edges) projected-pieces))))]
    (assoc a
           :center projected-center
           :center-edges (let
                          [ls (mapcat (fn [piece]
                                        [(first (:edges piece))
                                         (:center piece)])
                                      projected-pieces)]
                           (conj (vec ls) (first ls)))
           :pieces projected-pieces
           :centered? centered?)))

(defn transform-data [geometry rotation]
  (sort-by (comp not :centered?)
           (mapv (comp (partial project true projection/stereographic)
                       (partial cube/rotate-square
                                (quaternion/matrix-vector-product
                                 rotation)))
                 geometry)))
