(ns rubik.math.matrix
  (:refer-clojure :exclude [identity])
  (:require [rubik.math.vector :as vector]))

(defn integral [m]
  (mapv vector/integral m))

(defn vector-product
  [[[a1 a2 a3]
    [b1 b2 b3]
    [c1 c2 c3]]]
  (fn [[x y z]]
    [(+ (* x a1) (* y a2) (* z a3))
     (+ (* x b1) (* y b2) (* z b3))
     (+ (* x c1) (* y c2) (* z c3))]))
