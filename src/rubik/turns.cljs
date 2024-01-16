(ns rubik.turns
  (:require [rubik.math.random :as random]
            [rubik.math.vector :as vector]
            [rubik.math.quaternion :as quaternion]))

(defn significant-index [n vec]
  (if (empty? vec)
    n
    (if (zero? (first vec))
      (recur (inc n) (rest vec))
      n)))

(def axes
  [[1 0 0]
   [0 1 0]
   [0 0 1]
   [-1 0 0]
   [0 -1 0]
   [0 0 -1]])

(defn exclude-axes [axes excluded]
  (filter (fn [a]
            (not (some #{a} excluded)))
          axes))

(defn closest-axis [axis axes]
  (first (sort-by (partial vector/squared-distance axis)
                  axes)))

(defn turn-axis [turn]
  (and turn
       (first (:data turn))))

(defn initiate [time axis coordinates]
  (let
   [n (significant-index 0 axis)
    coord (nth coordinates n)]
    {:data [axis n coord]
     :time-total time
     :time-left time}))

(defn random-turn [time previous-axis]
  (let
   [axis (random/random-in
          (if previous-axis
            (exclude-axes axes [previous-axis (vector/scale-by -1 previous-axis)])
            axes))]
    (initiate time axis (random/random-in [(repeat -1) (repeat 0) (repeat 1)]))))

(defn turn-partial [turn]
  (let
   [{[axis n coord] :data
     time-total :time-total
     time-left :time-left} turn
    square (fn [a] (* a a))
    half-time (* 0.5 time-total)
    acceleration (/ Math/PI (square half-time))
    angle (* 0.5
             (if (> time-left half-time)
               (* 0.5 acceleration (square (- (* 2 half-time) time-left)))
               (- Math/PI
                  (* 0.5 acceleration (square time-left)))))]
    (fn [{:keys [coordinates]}]
      (if (= coord (nth coordinates n))
        (quaternion/from-axis-angle axis angle)
        quaternion/identity))))

(defn turn-geometry [geometry turn]
  (let
   [{[axis n coord] :data} turn
    colors (reduce (fn [h {:keys [coordinates normal] :as square}]
                     (assoc h [coordinates normal] (:color square)))
                   {}
                   geometry)
    turn (quaternion/integral-matrix-vector-product (quaternion/from-axis-angle axis (* -0.5 Math/PI)))]
    (mapv (fn [{:keys [coordinates normal] :as square}]
            (if (= coord (nth coordinates n))
              (assoc square :color (get colors [(turn coordinates) (turn normal)]))
              square))
          geometry)))
