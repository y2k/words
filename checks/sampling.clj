(ns words.sampling-check
  (:require [words.sampling :as sampling]))

(gen-class
 :name Runner
 :extends Object
 :methods [[main [] void]])

(defn- check [condition message]
  (if (not condition)
    (sneaky-throw (RuntimeException. (cast String message)))))

(defn- check-boundaries []
  (let [items [["A" 2] ["B" 0] ["C" 3]]
        selected (map
                  (fn [ticket]
                    (get-in
                     (sampling/sample! items 1
                                       (fn [entry] (get entry 1))
                                       (fn [total]
                                         (check (= total 5) "Wrong total")
                                         ticket))
                     [:items 0 0]))
                  [0 1 2 3 4])]
    (check (= selected ["A" "A" "C" "C" "C"])
           "Weighted interval boundaries")))

(defn- check-without-replacement []
  (let [items [["A" 2] ["B" 0] ["C" 3]]
        bounds (atom [])
        weighted (atom [])
        result (sampling/sample!
                items 2
                (fn [entry]
                  (swap! weighted (fn [seen] (concat seen [(get entry 0)])))
                  (get entry 1))
                (fn [total]
                  (check (= (deref weighted) ["A" "B" "C"])
                         "Weights must be computed once before sampling")
                  (swap! bounds (fn [seen] (concat seen [total])))
                  0))]
    (check (= (get result :items) [["A" 2] ["C" 3]]) "Selection without replacement")
    (check (= (deref bounds) [5 3]) "Selected weight must be removed")
    (check (= items [["A" 2] ["B" 0] ["C" 3]]) "Input must not change")
    (check (= (deref weighted) ["A" "B" "C"]) "Weight callback count"))
  (check (= (get (sampling/sample! ["A" "B" "C"] 3
                                  (fn [_] 1) (fn [total] (- total 1))) :items)
            ["C" "B" "A"])
         "Uniform sampling of all positions")
  (check (= (get (sampling/sample! ["same" "same"] 2
                                  (fn [_] 1) (fn [_] 0)) :items)
            ["same" "same"])
         "Equal values in distinct positions must not be deduplicated"))

(defn- check-errors []
  (map
   (fn [amount]
     (let [result (sampling/sample! ["item"] amount
                                     (fn [_] (check false "Invalid amount evaluated weights"))
                                     (fn [_] (check false "Invalid amount used random")))]
       (check (= (get result :error) :invalid-amount) "Invalid amount accepted")))
   [-1 0.5 "1" nil])
  (map
   (fn [items]
     (check (= (get (sampling/sample! items 0
                                     (fn [_] (check false "Zero amount evaluated weights"))
                                     (fn [_] (check false "Zero amount used random"))) :items)
               [])
            "Zero amount must return an empty selection"))
   [[] ["item"]])
  (map
   (fn [weight]
     (let [result (sampling/sample! [weight] 1 (fn [item] item)
                                     (fn [_] (check false "Invalid weight used random")))]
       (check (= (get result :error) :invalid-weight) "Invalid weight accepted")))
   [-1 0.5 "1000" nil])
  (map
   (fn [[weights amount]]
     (let [result (sampling/sample! weights amount (fn [item] item)
                                     (fn [_] (check false "Insufficient items used random")))]
       (check (= (get result :error) :insufficient-items)
              "Insufficient positive weights accepted")))
   [[[] 1] [[0 0] 1] [[0 2 0] 2] [[1] 2]])
  (check (= (get (sampling/sample! [2147483647 1] 1 (fn [item] item)
                                  (fn [_] (check false "Overflow used random"))) :error)
            :weight-overflow)
         "Weight overflow accepted")
  (check (= (get (sampling/sample! [2147483647 0] 1 (fn [item] item)
                                  (fn [total]
                                    (check (= total 2147483647) "Max integer total")
                                    (- total 1))) :items)
            [2147483647])
         "Maximum integer weight rejected"))

(defn -main [this]
  (check-boundaries)
  (check-without-replacement)
  (check-errors)
  nil)
