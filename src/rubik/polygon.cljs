(ns rubik.polygon)

(defn in-polygon? [[x y]]
  (let
   [min-max (fn [a b]
              (if (< a b) [a b] [b a]))
    intersects? (fn [[a b]]
                  (let
                   [[ax ay] a
                    [bx by] b
                    [ax bx] (min-max ax bx)
                    [ay by] (min-max ay by)
                    r (/ (- y ay)
                         (- by ay))]
                    (and (< ay y by)
                         (< x (+ ax (* r (- bx ax)))))))]
    (fn [ls]
      (= 1 (count (filter intersects? ls))))))

(defn to-polygon [edges]
  (let
   [first-edge (first edges)]
    (loop [res [] a first-edge ls (rest edges)]
      (if (seq ls)
        (let
         [b (first ls)]
          (recur (cons [a b] res) b (rest ls)))
        (reverse (cons [a first-edge] res))))))
