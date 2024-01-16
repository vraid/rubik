(ns rubik.subs
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-sub
 ::draw-data
 (fn [db]
   (assoc (:draw-data db)
          :geometry (:geometry db)
          :scale (:scale db))))

(re-frame/reg-sub
 ::initial-scramble
 :initial-scramble)

(re-frame/reg-sub
 ::initial-scramble-count
 :initial-scramble-count)

(re-frame/reg-sub
 ::rotation-disabled?
 (fn [db]
   (:disabled? (:rotation db))))
