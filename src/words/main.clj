(ns words.main
  (:require [words.app :as app])
  (:require [words.view :as view])
  (:import [android.app Activity]
           [android.os Bundle]
           [android.view View]
           [android.widget LinearLayout ScrollView]
           [java.util ArrayList Collection Collections]))

(gen-class
 :name MainActivity
 :extends Activity
 :methods [[^override onCreate [Bundle] void]])

(defn- shuffle-list! [items]
  (let [copy (ArrayList. (cast Collection items))]
    (Collections/shuffle copy)
    copy))

(defn -onCreate [^MainActivity self savedInstanceState]
  (let [scroll (ScrollView. self)
        root (LinearLayout. self)]
    (.setOrientation root 1)
    (.setFitsSystemWindows scroll true)
    (.addView scroll root)
    (.setContentView self scroll)
    ((app/main)
     {:context self
      :shuffle-list (fn [_w items] (shuffle-list! items))
      :update-ui (fn [w node]
                   (let [f (view/create node)]
                     (.addView root (cast View (f w)))
                     (.post scroll
                            ^void:java.lang.Runnable
                            (fn [] (.scrollTo scroll 0 (.getBottom root)))))
                   nil)})) nil)
