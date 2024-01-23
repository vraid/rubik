(ns rubik.events
  (:require [re-frame.core :as re-frame]
            [rubik.db :as db]
            [rubik.cube :as cube]
            [rubik.turns :as turns]
            [rubik.select :as select]
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
   (assoc-in db [:graphics :shader] shader)))

(re-frame/reg-event-db
 ::control
 (fn [db [_ a]]
   (assoc db :control a)))

(re-frame/reg-event-db
 ::reset
 (fn [db _]
   (-> db
       (update :geometry #(mapv cube/reset-color %))
       (assoc :past-turns []))))

(re-frame/reg-event-db
 ::disable-rotation
 (fn [db [_ a]]
   (assoc-in db [:rotation :disabled?] a)))

(re-frame/reg-event-fx
 ::start-in
 (fn [cofx [_ ms]]
   (assoc cofx :fx [[:dispatch-later {:ms ms :dispatch [::control :initial]}]])))

(defn apply-turning [db]
  (if (not (:turning db))
    db
    (let
     [turn (:turning db)
      {time-left :time-left} turn
      time-per-frame (:time-per-frame db)
      time-left (- time-left time-per-frame)]
      (if (> time-left 0)
        (-> db
            (assoc-in [:turning :time-left] time-left)
            (assoc-in [:graphics :square-rotation] (turns/turn-partial (assoc (:turning db) :time-left time-left))))
        (let
         [turned (turns/turn-geometry (:geometry db) turn)]
          (-> db
              (assoc :turning false)
              (update :initial-scramble #(if (seq %) (rest %) []))
              (assoc :geometry turned)
              (assoc-in [:graphics :geometry] turned)
              (assoc-in [:graphics :square-rotation] (fn [_] quaternion/identity))))))))

(defn apply-rotation [db]
  (let
   [rotation (:rotation db)
    axis (:axis rotation)
    quat (quaternion/product
          (quaternion/from-axis-angle
           axis
           (if (or (:disabled? rotation) (:paused? rotation))
             0
             (:speed rotation)))
          (:perspective db))]
    (-> db
        (assoc :perspective quat)
        (assoc-in [:graphics :perspective] quat))))

(defn apply-next-turn [db current-turn]
  (let
   [initial (:initial-scramble db)
    turn (:turning db)
    past (:past-turns db)
    control (:control db)
    with-past (fn [a] [a (cons (:data a) past)])
    state (if turn
            [turn past]
            (case control
              :initial (and (seq initial) (with-past (first initial)))
              :scramble (with-past (turns/random-turn (:time-to-turn db) (turns/turn-axis current-turn)))
              :rewind (and (seq past) [(turns/reverse-turn (:time-to-turn db) (first past)) (rest past)])
              false))]
    (if (not state)
      (assoc db :control (if (= :none control) :none :manual))
      (let
       [[next-turn past] state]
        (-> db
            (assoc :turning next-turn)
            (assoc :past-turns past))))))

(re-frame/reg-event-fx
 ::tick
 (fn [cofx _]
   (let
    [db (:db cofx)
     time (:time-per-frame db)]
     {:db (-> db
              apply-rotation
              apply-turning
              (apply-next-turn (:turning db)))
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
   (if (:touch-start db)
     db
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
                           :vector vec))))))))

(defn initiate-turn [bounding-rect initial coord]
  (fn [db]
    (let
     [selected (select/selected-square (:geometry db) initial)
      normal (:normal selected)
      current (target-point (:scale db) (:perspective db) bounding-rect coord)
      cross (vector/cross-product-normal initial current)
      axis (turns/closest-axis cross (turns/exclude-axes turns/axes [normal (vector/scale-by -1 normal)]))
      turn (and (not (vector/invalid? cross))
                (turns/initiate (:time-to-turn db) axis (:coordinates selected)))]
      (-> db
          (assoc :turning turn)
          (update :past-turns #(if turn (cons (:data turn) %) %))))))

(re-frame/reg-event-db
 ::mouse-up
 (fn [db [_ coord extra]]
   (let
    [mouse-down (:mouse-down db)
     end-mouse-down? (and mouse-down
                          (= (:button mouse-down)
                             (:button extra)))
     initiate-turn (if (and (= :manual (:control db))
                            (not (:turning db))
                            (not (:ctrl? mouse-down))
                            end-mouse-down?)
                     (initiate-turn (:bounding-rect mouse-down)
                                    (:vector mouse-down)
                                    coord)
                     identity)]
     (-> db
         initiate-turn
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

(re-frame/reg-event-db
 ::touch-start
 (fn [db [_ touches changed extra]]
   (if (:mouse-down db)
     db
     (let
      [bounding-rect (:bounding-rect extra)]
       (-> db
           (assoc :touch-start
                  {:bounding-rect bounding-rect
                   :touches touches
                   :vectors (into {}
                                  (map (fn [[id pt]]
                                         [id (target-point (:scale db) (:perspective db) bounding-rect pt)])
                                       touches))})
           (assoc :touch-event [:start touches changed]))))))

(re-frame/reg-event-db
 ::touch-end
 (fn [db [_ touches changed]]
   (let
    [touch-start (:touch-start db)
     first-coords (comp second first)
     initiate-turn (if (and (= :manual (:control db))
                            (not (:turning db))
                            touch-start
                            (= 1 (count (:touches touch-start)))
                            (empty? touches))
                     (initiate-turn (:bounding-rect touch-start)
                                    (first-coords (:vectors touch-start))
                                    (first-coords changed))
                     identity)]
     (-> db
         initiate-turn
         (assoc :touch-event [:end touches changed])
         (update :touch-start #(and (seq touches) %))
         (assoc-in [:rotation :paused?] false)))))

(re-frame/reg-event-db
 ::touch-move
 (fn [db [_ touches changed]]
   (let
    [touch-start (:touch-start db)
     [_ _ prev] (:touch-event db)
     average (fn [d]
               (vector/scale-by
                (/ 1 (count d))
                (reduce vector/sum [0 0] (vals d))))
     [rotation axis] (update-rotation
                      (and touch-start
                           (= 2 (count prev))
                           (= 2 (count changed)))
                      (:bounding-rect touch-start)
                      (:scale db)
                      (:perspective db)
                      (:axis (:rotation db))
                      (average changed)
                      (average prev))]
     (-> db
         (assoc :touch-event [:move touches changed])
         (assoc-in [:rotation :axis] axis)
         (assoc-in [:rotation :paused?] (= 2 (count changed)))
         (assoc :perspective rotation)))))

(re-frame/reg-event-db
 ::touch-cancel
 (fn [db [_ touches changed]]
   (-> db
       (assoc :touch-event [:cancel touches changed])
       (update :touch-start #(and % (seq touches)))
       (assoc-in [:rotation :paused?] false))))
