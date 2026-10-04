(ns words.main
  (:require [words.app :as app])
  (:require [words.view :as view])
  (:import [android.app Activity]
           [android.os Bundle]
           [android.view View]
           [android.widget LinearLayout ScrollView]
           [java.util Random]))

(gen-class
 :name MainActivity
 :extends Activity
 :methods [[^override onCreate [Bundle] void]])

(defn -onCreate [^MainActivity self savedInstanceState]
  (let [random (Random.)
        scroll (ScrollView. self)
        root (LinearLayout. self)
        padding (cast int (view/dp self 16))]
    (.setOrientation root 1)
    (.setGravity root 80) ; Gravity.BOTTOM
    (.setPadding root padding padding padding padding)
    (.setFillViewport scroll true)
    (.setFitsSystemWindows scroll true)
    (.addView scroll root)
    (.setContentView self scroll)
    ((app/main)
     {:context self
      :random-seed (fn [_w _arg] (.nextInt random))
      :clear-ui (fn [_w _arg] (.removeAllViews root) nil)
      :update-ui (fn [w node]
                   (let [f (view/create node)]
                     (view/disable-children! root 0)
                     (.addView root (cast View (f w)))
                     (if (> (.getChildCount root) 15)
                       (.removeViewAt root 0))
                     (.post scroll
                            ^void:java.lang.Runnable
                            (fn [] (.scrollTo scroll 0 (.getBottom root)))))
                   nil)})) nil)
