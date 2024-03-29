(ns rubik.math.quaternion
  (:refer-clojure :exclude [identity])
  (:require [rubik.math.vector :as vector]
            [rubik.math.matrix :as matrix]))

(def identity [1 0 0 0])

(def normal vector/normal)

(defn conjugate [[a b c d]]
  [a (- b) (- c) (- d)])

(defn product
  [[w x y z]
   [a b c d]]
  [(- (* w a) (+ (* x b) (* y c) (* z d)))
   (- (+ (* w b) (* x a) (* y d)) (* z c))
   (- (+ (* w c) (* y a) (* z b)) (* x d))
   (- (+ (* w d) (* x c) (* z a)) (* y b))])

(def product-normal (comp normal product))

(defn vector-product [q v]
  (vec (rest (reduce product [q (cons 0 v) (conjugate q)]))))

(defn from-axis-angle [axis angle]
  (let
   [a (* 0.5 angle)]
    (normal (cons (Math/cos a)
                  (vector/scale-by (Math/sin a)
                                   (vector/normal axis))))))

(defn to-matrix [[a b c d]]
  (let
   [[b2 c2 d2] (map (fn [n] (* 2 n n)) [b c d])
    [ab ac ad] (map (partial * 2 a) [b c d])
    bd (* 2 b d)
    bc (* 2 b c)
    cd (* 2 c d)]
    [[(- 1 (+ c2 d2)) (- bc ad) (+ bd ac)]
     [(+ bc ad) (- 1 (+ b2 d2)) (- cd ab)]
     [(- bd ac) (+ cd ab) (- 1 (+ b2 c2))]]))

(def matrix-vector-product (comp matrix/vector-product to-matrix))
(def integral-matrix-vector-product (comp matrix/vector-product matrix/integral to-matrix))
