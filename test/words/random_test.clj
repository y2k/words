(ns words.random-test
  (:require [words.vendor.random :as random]))

(gen-class :name Runner :extends Object :methods [[main [] void]])

(defn- check [condition message]
  (if (not condition)
    (sneaky-throw (RuntimeException. (cast String message)))))

(defn -main [this]
  ;; Reference sequence from java.util.Random: bounded draw, then nextInt for the next seed.
  (let [first-draw (random/next-int 42 5)
        second-draw (random/next-int (get first-draw :seed) 3)
        third-draw (random/next-int (get second-draw :seed) 1)]
    (check (= {:value 0 :seed 234785527} first-draw) "First random step")
    (check (= {:value 0 :seed 1768415418} second-draw) "Seed continuation")
    (check (= {:value 0 :seed -1198127336} third-draw) "Bound one must advance seed")
    (check (= first-draw (random/next-int 42 5)) "Random step must be reproducible"))
  (map (fn [seed]
         (map (fn [bound]
                (let [result (random/next-int seed bound)
                      value (get result :value)]
                  (check (and (>= value 0) (< value bound)) "Random value outside bounds")
                  (check (instance? Integer (get result :seed)) "Next seed must be an integer")
                  (check (= result (random/next-int seed bound)) "Random state leaked")))
              [1 2 5 2147483647]))
       [0 1 -1 2147483647 -2147483648])
  nil)
