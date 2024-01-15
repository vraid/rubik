(ns rubik.math.projection)

(defn stereographic [[x y z]]
  (let
   [scale (fn [a] (/ a (- 1 z)))]
    #js [(scale x) (scale y) 0]))

(defn inverse-stereographic [[x y]]
  (let
   [square (+ (* x x) (* y y))
    z (/ (dec square) (inc square))
    scale (partial * (- 1 z))]
    [(scale x) (scale y) z]))

(defn orthographic [a]
  (let
   [[x y _] a]
    #js [x y 0]))
