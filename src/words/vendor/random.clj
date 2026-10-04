(ns words.vendor.random
  (:import [java.util Random]))

(defn next-int [seed bound]
  ;; The second draw becomes the explicit seed for the next call.
  (let [random (Random. (cast long (cast int seed)))
        value (.nextInt random (cast int bound))
        next-seed (.nextInt random)]
    {:value value :seed next-seed}))
