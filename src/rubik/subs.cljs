(ns rubik.subs
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-sub
 ::draw-data
 (fn [db]
   (assoc (:draw-data db)
          :geometry (:geometry db)
          :scale (:scale db))))
