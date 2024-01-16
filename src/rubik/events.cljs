(ns rubik.events
  (:require [re-frame.core :as re-frame]
            [rubik.db :as db]
            [rubik.math.vector :as vector]
            [rubik.math.quaternion :as quaternion]
            [rubik.math.projection :as projection]))

(re-frame/reg-event-db
 ::initialize-db
 (fn [_ _]
   db/default-db))

(re-frame/reg-event-db
 ::set-shader
 (fn [db [_ shader]]
   (assoc db :shader shader)))

(defn apply-rotation [db]
  (let
   [rotation (:rotation db)
    axis (:axis rotation)
    rotate (if (:paused? rotation)
             identity
             (partial quaternion/product
                      (quaternion/from-axis-angle
                       axis
                       (:speed rotation))))]
    (update db :perspective rotate)))

(re-frame/reg-event-fx
 ::tick
 (fn [cofx _]
   (let
    [db (:db cofx)
     time (:time-per-frame db)]
     {:db (apply-rotation db)
      :fx [[:dispatch-later {:ms time :dispatch [::tick]}]]})))

(defn translate-scale [offset window value]
  (/ (- value (+ offset (* 0.5 window)))
     (* 0.5 window)))

(defn adjust-to-bounding-rect [[x y width height]]
  (fn [[ax ay]]
    [(translate-scale x width ax)
     (- (translate-scale y height ay))]))

(defn point-to-sphere [bounding-rect scale]
  (comp projection/inverse-stereographic
        (partial vector/scale-by scale)
        (adjust-to-bounding-rect bounding-rect)))

(defn target-point [scale rotation bounding-rect coord]
  (quaternion/vector-product
   (quaternion/conjugate rotation)
   ((point-to-sphere bounding-rect scale)
    coord)))

(re-frame/reg-event-db
 ::mouse-down
 (fn [db [_ coord extra]]
   (let
    [register? (= 0 (:button extra))
     mouse-down (:mouse-down db)
     vec (target-point (:scale db) (:perspective db) (:bounding-rect extra) coord)]
     (-> db
         (assoc :mouse-event [:down coord extra])
         (assoc :mouse-down
                (if (or mouse-down
                        (not register?))
                  mouse-down
                  (assoc extra
                         :coord coord
                         :vector vec)))))))


(re-frame/reg-event-db
 ::mouse-up
 (fn [db [_ coord extra]]
   (let
    [mouse-down (:mouse-down db)
     end-mouse-down? (and mouse-down
                          (= (:button mouse-down)
                             (:button extra)))]
     (-> db
         (update-in [:rotation :paused?] #(and % (not end-mouse-down?)))
         (assoc :mouse-event [:up coord extra])
         (assoc :mouse-down (and (not end-mouse-down?)
                                 mouse-down))))))

(defn update-rotation [condition bounding-rect scale rotation rotation-axis coord prev]
  (if (not condition)
    [rotation rotation-axis]
    (let
     [[to from] (map (point-to-sphere bounding-rect scale)
                     [coord prev])
      axis (vector/cross-product-normal from to)
      angle (vector/angle-between from to)]
      (if (vector/invalid? axis)
        [rotation rotation-axis]
        [(quaternion/product-normal (quaternion/from-axis-angle axis angle) rotation) axis]))))

(re-frame/reg-event-db
 ::mouse-move
 (fn [db [_ coord]]
   (let
    [mouse-down (:mouse-down db)
     drag? (and mouse-down
                (= (:button mouse-down) 0)
                (:ctrl? mouse-down))
     [_ prev] (:mouse-event db)
     [rotation axis] (update-rotation
                      drag?
                      (:bounding-rect mouse-down)
                      (:scale db)
                      (:perspective db)
                      (:axis (:rotation db))
                      coord
                      prev)]
     (-> db
         (assoc-in [:rotation :axis] axis)
         (update-in [:rotation :paused?] #(or % drag?))
         (assoc :mouse-event [:move coord prev])
         (assoc :perspective rotation)))))
