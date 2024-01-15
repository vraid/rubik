(ns rubik.cube
  (:require [rubik.math.vector :as vector]
            [rubik.math.quaternion :as quaternion]))

(defn square [a] (* a a))

(defn stepwise-vertices [vertices axis angle vector]
  (mapv (fn [a]
          (quaternion/vector-product
           (quaternion/from-axis-angle axis a)
           vector))
        (map (partial * (/ angle (dec (* 2 vertices))))
             (range (* 2 vertices)))))

(defn rotate-piece [rotated {:keys [center edges]}]
  {:center (rotated center)
   :edges (mapv rotated edges)})

(defn create-piece [vertices center corner [a-axis a-angle] [b-axis b-angle]]
  (let
   [stepwise (partial stepwise-vertices vertices)]
    {:center (vector/normal (vector/sum corner (vector/scale-by 0.6 center)))
     :edges (concat (reverse (rest (stepwise a-axis (- a-angle) corner)))
                    (stepwise b-axis b-angle corner))}))

(defn center-square [settings]
  (let
   [{spacing :spacing
     vertices :vertices} settings
    {space-inner :inner} spacing
    base-length (vector/scalar-remainder 1 [space-inner])
    base-xy space-inner
    base-z (vector/scalar-remainder (square base-length) [base-xy])
    angle (Math/atan2 base-xy base-z)
    corner [base-xy base-xy (- base-z)]
    rotations (map (comp (partial quaternion/from-axis-angle [0 0 1])
                         (partial * 0.5 Math/PI))
                   (range 4))
    center [0 0 -1]
    piece (create-piece vertices center corner [[1 0 0] angle] [[0 1 0] angle])]
    {:coordinates [0 0 -1]
     :normal [0 0 -1]
     :center center
     :pieces
     (mapv (fn [rotation]
             (rotate-piece (partial quaternion/vector-product rotation) piece))
           rotations)}))

(defn top-square [settings]
  (let
   [{spacing :spacing
     vertices :vertices} settings
    {space-inner :inner
     space-outer :outer
     space-width :width} spacing
    bottom-x [space-inner 0 0]
    bottom-y [0 space-outer 0]
    bottom-z [0 0 (- (vector/remainder 1 [bottom-x bottom-y]))]
    top-x [space-inner 0 0]
    top-z (vector/scale-to space-width [0 -1 -1])
    top-y (vector/scale-to (vector/remainder 1 [top-x top-z]) [0 1 -1])
    bottom-axis-angle [[0 -1 0] (vector/atan2 bottom-x bottom-z)]
    top-axis-angle [[0 1 1] (vector/atan2 top-x top-y)]
    side-angle (let
                [top-vector (reduce vector/sum [top-x top-y top-z])
                 [_ top-y top-z] top-vector]
                 (* 0.5 (- (Math/atan2 top-y (Math/abs top-z))
                           (vector/atan2 bottom-y bottom-z))))
    right-axis-angle [[1 0 0] side-angle]
    left-axis-angle [[-1 0 0] side-angle]
    center (vector/normal (reduce vector/sum [bottom-y bottom-z top-y top-z]))
    create-piece (partial create-piece vertices center)
    bottom-left (reduce vector/sum [(vector/scale-by -1 bottom-x) bottom-y bottom-z])
    bottom-right (reduce vector/sum [bottom-x bottom-y bottom-z])
    top-right (reduce vector/sum [top-x top-y top-z])
    top-left (reduce vector/sum [(vector/scale-by -1 top-x) top-y top-z])]
    {:coordinates [0 1 -1]
     :normal [0 0 -1]
     :center center
     :pieces
     [(create-piece bottom-right bottom-axis-angle right-axis-angle)
      (create-piece top-right right-axis-angle top-axis-angle)
      (create-piece top-left top-axis-angle left-axis-angle)
      (create-piece bottom-left left-axis-angle bottom-axis-angle)]}))

(defn corner-square [settings]
  (let
   [{spacing :spacing
     vertices :vertices} settings
    {space-outer :outer
     space-width :width} spacing
    bottom-left (let
                 [xy space-outer]
                  [xy xy (- (vector/scalar-remainder 1 [xy xy]))])
    top-right (let
               [k (* space-width (Math/sqrt 2))
                xy (* (/ 1 3)
                      (- (Math/sqrt (- 3 (* 2 (square k))))
                         k))
                z (+ xy k)]
                [xy xy (- z)])
    side-vector (fn [a b c]
                  (let
                   [x (vector/scale-to space-outer a)
                    z (vector/scale-to space-width c)
                    y (vector/scale-to (vector/remainder 1 [x z]) b)]
                    (reduce vector/sum [x y z])))
    top-left (side-vector [1 0 0] [0 1 -1] [0 -1 -1])
    bottom-right (side-vector [0 1 0] [1 0 -1] [-1 0 -1])
    center (vector/normal (vector/sum bottom-left top-right))
    inner-angle (let
                 [[_ top-y top-z] top-left
                  [_ bottom-y bottom-z] bottom-left]
                  (* 0.5 (- (Math/atan2 top-y (Math/abs top-z))
                            (Math/atan2 bottom-y (Math/abs bottom-z)))))
    outer-angle (let
                 [[_ y _] top-right]
                  (* 0.5 (- (Math/atan2 y (vector/scalar-remainder 1 [y space-width]))
                            (Math/atan2 space-outer (vector/scalar-remainder 1 [space-outer space-width])))))
    bottom-axis-angle [[0 -1 0] inner-angle]
    right-axis-angle [[1 0 1] outer-angle]
    top-axis-angle [[0 1 1] outer-angle]
    left-axis-angle [[-1 0 0] inner-angle]
    create-piece (partial create-piece vertices center)]
    {:coordinates [1 1 -1]
     :normal [0 0 -1]
     :center center
     :pieces
     [(create-piece bottom-right bottom-axis-angle right-axis-angle)
      (create-piece top-right right-axis-angle top-axis-angle)
      (create-piece top-left top-axis-angle left-axis-angle)
      (create-piece bottom-left left-axis-angle bottom-axis-angle)]}))

(defn rotate-square [rotated {:keys [coordinates normal center pieces] :as square}]
  (assoc square
         :coordinates (rotated coordinates)
         :normal (rotated normal)
         :center (rotated center)
         :pieces (mapv (partial rotate-piece rotated) pieces)))

(defn with-distance [{:keys [center pieces] :as square}]
  (let
   [distance (map (partial vector/squared-distance center) (mapcat :edges pieces))]
    (assoc square
           :max-edge-distance (reduce max 0 distance)
           :min-edge-distance (reduce min 4 distance))))

(defn quadruplicate [square]
  (mapv (fn [angle]
          (rotate-square
           (quaternion/integral-matrix-vector-product
            (quaternion/from-axis-angle [0 0 -1] angle))
           square))
        (map (partial * 0.5 Math/PI)
             (range 4))))

(defn geometry [settings]
  (let
   [face (map with-distance
              (concat [(center-square settings)]
                      (quadruplicate (top-square settings))
                      (quadruplicate (corner-square settings))))
    rotation (comp quaternion/integral-matrix-vector-product
                   quaternion/from-axis-angle)]
    (mapcat (fn [[color rotation]]
              (mapv (fn [square]
                      (assoc (rotate-square rotation square)
                             :color color))
                    face))
            [[#js [1 1 1 1] (rotation [1 0 0] 0)]
             [#js [1 0 0 1] (rotation [0 1 0] (* 0.5 Math/PI))]
             [#js [0 0 1 1] (rotation [-1 0 0] (* 0.5 Math/PI))]
             [#js [0 1 0 1] (rotation [1 0 0] (* 0.5 Math/PI))]
             [#js [1 0 1 1] (rotation [0 -1 0] (* 0.5 Math/PI))]
             [#js [1 1 0 1] (rotation [0 1 0] Math/PI)]])))
