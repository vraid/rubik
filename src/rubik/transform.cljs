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

(defn project [center? projection]
  (fn [{:keys [center pieces max-edge-distance min-edge-distance] :as a}]
    (let
     [projected-center (projection center)
      pole-distance (partial vector/squared-distance [0 0 1])
      center-distance (pole-distance center)
      close? (< center-distance max-edge-distance)
      not-close-enough? (> center-distance min-edge-distance)
      proj (if (not (and close? not-close-enough?))
             projection
             (comp projection
                   (fn [a]
                     (let
                      [dist (pole-distance a)
                       limit 0.04]
                       (if (< limit dist)
                         a
                       ; try to prevent visual artifacts
                         (vector/normal
                          (vector/sum a (vector/scale-to
                                         (* -3 (- limit dist))
                                         (vector/subtract center a)))))))))
      projected-pieces (mapv (project-piece proj)
                             (if (and close? not-close-enough?)
                               (filter (fn [a]
                                         (< 0.00000001 (reduce min 4 (mapv pole-distance (:edges a)))))
                                       pieces)
                               pieces))
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
             :centered? centered?))))

(defn transform-data [geometry rotation square-rotation]
  (sort-by (comp not :centered?)
           (mapv (comp (project true projection/stereographic)
                       (fn [square]
                         (cube/rotate-square
                          (quaternion/matrix-vector-product
                           (quaternion/product rotation
                                               (square-rotation square)))
                          square)))
                 geometry)))
