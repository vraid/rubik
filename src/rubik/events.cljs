(ns rubik.events
  (:require [re-frame.core :as re-frame]
            [rubik.db :as db]
            [rubik.math.quaternion :as quaternion]))

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
    rotate (partial quaternion/product
                    (quaternion/from-axis-angle
                     axis
                     (:speed rotation)))]
    (update db :perspective rotate)))

(re-frame/reg-event-fx
 ::tick
 (fn [cofx _]
   (let
    [db (:db cofx)
     time (:time-per-frame db)]
     {:db (apply-rotation db)
      :fx [[:dispatch-later {:ms time :dispatch [::tick]}]]})))
