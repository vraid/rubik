(ns rubik.select
  (:require [rubik.cube :as cube]
            [rubik.polygon :as polygon]
            [rubik.math.vector :as vector]
            [rubik.math.quaternion :as quaternion]))

(defn selected-square [geometry vec]
  (or (reduce (fn [result {:keys [center] :as square}]
                (or result
                    (let
                     [center center
                      axis (vector/cross-product-normal center [0 0 -1])
                      rotate (quaternion/integral-matrix-vector-product
                              (if (vector/invalid? axis)
                                quaternion/identity
                                (quaternion/from-axis-angle axis (vector/angle-between center [0 0 -1]))))
                      [x y] (rotate vec)
                      rotated (cube/rotate-square rotate square)]
                      (and ((polygon/in-polygon? [x y]) (polygon/to-polygon (mapcat (comp rest :edges) (:pieces rotated))))
                           square))))
              false
              (filter (fn [{:keys [center max-edge-distance]}]
                        (<= (vector/squared-distance center vec) max-edge-distance))
                      geometry))
      (first (sort-by (fn [{:keys [center]}]
                        (vector/squared-distance center vec))
                      geometry))))
