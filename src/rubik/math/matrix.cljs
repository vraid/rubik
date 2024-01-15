(ns rubik.math.matrix
  (:refer-clojure :exclude [identity]))

(defn by-row [f]
  (fn [a b]
    (assert (= (count a) (count b)))
    (mapv f a b)))

(defn by-element [f]
  (fn [a b]
    (assert (= (count a) (count b)))
    (mapv (by-row f) a b)))

(def sum (by-element +))

(defn scale-by [factor a]
  (mapv (fn [row] (mapv (partial * factor) row)) a))

(defn diagonal [values]
  (let
   [size (count values)]
    (mapv (fn [n]
            (vec (concat (repeat n 0) [(nth values n)] (repeat (- size (inc n)) 0))))
          (range size))))

(defn identity [size]
  (diagonal (repeat size 1)))

(defn vector-product [m]
  (let
   [[a b c] m
    [a1 a2 a3] a
    [b1 b2 b3] b
    [c1 c2 c3] c]
    (fn [[x y z]]
      [(+ (* x a1) (* y a2) (* z a3))
       (+ (* x b1) (* y b2) (* z b3))
       (+ (* x c1) (* y c2) (* z c3))])))
