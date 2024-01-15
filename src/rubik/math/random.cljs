(ns rubik.math.random)

(defn random-angle []
  (* 2 Math/PI (Math/random)))

(defn random-axis []
  (let
   [z (- (* 2 (Math/random)) 1)
    xy (Math/sqrt (- 1 (* z z)))
    angle (random-angle)]
    [(* xy (Math/cos angle)) (* xy (Math/sin angle)) z]))
