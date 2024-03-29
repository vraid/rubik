(ns rubik.math.projection)

(defn stereographic [[x y z]]
  #js [(/ x (- 1 z))
       (/ y (- 1 z))
       0])

(defn inverse-stereographic [[x y]]
  (let
   [square (+ (* x x) (* y y))
    z (/ (dec square) (inc square))
    scale (partial * (- 1 z))]
    [(scale x) (scale y) z]))
