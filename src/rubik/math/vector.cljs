(ns rubik.math.vector)

(defn invalid? [a]
  (js/isNaN (reduce + 0 a)))

(defn integral [a]
  (mapv Math/round a))

(defn square [a]
  (* a a))

(defn squared-length [a]
  (reduce + 0 (map (fn [n] (square n)) a)))

(defn length [a]
  (Math/sqrt (squared-length a)))

(defn scale-by [factor a]
  (mapv (partial * factor) a))

(defn scale-to [k a]
  (scale-by (/ k (length a)) a))

(defn normal [a]
  (scale-to 1 a))

(defn sum [a b]
  (mapv + a b))

(defn subtract [a b]
  (mapv - b a))

(defn squared-distance [a b]
  (squared-length (subtract a b)))

(defn dot-product [a b]
  (reduce + 0 (mapv * a b)))

(defn cross-product-normal [a b]
  (let
   [[a1 a2 a3] a
    [b1 b2 b3] b]
    (normal [(- (* a2 b3) (* a3 b2))
             (- (* a3 b1) (* a1 b3))
             (- (* a1 b2) (* a2 b1))])))

(defn angle-between [a b]
  (Math/acos (/ (dot-product a b)
                (Math/sqrt (* (squared-length a)
                              (squared-length b))))))

(defn base-remainder [f length values]
  (Math/sqrt (- length (reduce + 0 (map f values)))))

(def scalar-remainder (partial base-remainder square))

(def remainder (partial base-remainder squared-length))

(defn atan2 [a b]
  (Math/atan2 (length a) (length b)))
