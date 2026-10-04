(ns words.sampling-test
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
                  (fn [seed]
                    (get-in
                     (sampling/sample! items 1
                                       (fn [entry] (get entry 1))
                                        seed)
                     [:items 0 0]))
                  [0 6 4 2 3])]
    (check (= selected ["A" "A" "C" "C" "C"])
           "Weighted interval boundaries")))

(defn- check-without-replacement []
  (let [items [["A" 2] ["B" 0] ["C" 3]]
        weighted (atom [])
        result (sampling/sample!
                items 2
                (fn [entry]
                  (swap! weighted (fn [seen] (concat seen [(get entry 0)])))
                  (get entry 1))
                42)]
    (check (= (get result :items) [["A" 2] ["C" 3]]) "Selection without replacement")
    (check (= (get result :seed) 1768415418) "Seed must advance after every selection")
    (check (= items [["A" 2] ["B" 0] ["C" 3]]) "Input must not change")
    (check (= (deref weighted) ["A" "B" "C"]) "Weight callback count"))
  (check (= (get (sampling/sample! ["A" "B" "C"] 3 (fn [_] 1) 42) :items)
            ["C" "B" "A"])
         "Uniform sampling of all positions")
  (check (= (get (sampling/sample! ["same" "same"] 2
                                  (fn [_] 1) 42) :items)
            ["same" "same"])
         "Equal values in distinct positions must not be deduplicated"))

(defn- check-errors []
  (map
   (fn [amount]
     (let [result (sampling/sample! ["item"] amount
                                     (fn [_] (check false "Invalid amount evaluated weights"))
                                      42)]
       (check (= (get result :error) :invalid-amount) "Invalid amount accepted")))
   [-1 0.5 "1" nil])
  (map
   (fn [items]
      (check (= (sampling/sample! items 0
                                      (fn [_] (check false "Zero amount evaluated weights"))
                                      42)
                {:items [] :seed 42})
             "Zero amount must return an empty selection and unchanged seed"))
   [[] ["item"]])
  (map
   (fn [weight]
     (let [result (sampling/sample! [weight] 1 (fn [item] item)
                                      42)]
       (check (= (get result :error) :invalid-weight) "Invalid weight accepted")))
   [-1 0.5 "1000" nil])
  (map
   (fn [[weights amount]]
     (let [result (sampling/sample! weights amount (fn [item] item)
                                      42)]
       (check (= (get result :error) :insufficient-items)
              "Insufficient positive weights accepted")))
   [[[] 1] [[0 0] 1] [[0 2 0] 2] [[1] 2]])
  (check (= (get (sampling/sample! [2147483647 1] 1 (fn [item] item)
                                  42) :error)
            :weight-overflow)
         "Weight overflow accepted")
  (check (= (get (sampling/sample! [2147483647 0] 1 (fn [item] item)
                                  42) :items)
            [2147483647])
         "Maximum integer weight rejected"))

(defn -main [this]
  (check-boundaries)
  (check-without-replacement)
  (check-errors)
  nil)
