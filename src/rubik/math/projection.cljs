(ns rubik.math.projection)

(defn stereographic [[x y z]]
  (let
   [scale (fn [a] (/ a (- 1 z)))]
    #js [(scale x) (scale y) 0]))

(defn orthographic [a]
  (let
   [[x y _] a]
    #js [x y 0]))
