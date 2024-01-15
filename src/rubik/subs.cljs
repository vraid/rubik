(ns rubik.subs
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-sub
 ::data
 (fn [db]
   {:geometry (:geometry db)
    :buffers (:buffers db)}))
