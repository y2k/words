(ns words.question-test
  (:require [words.question :as question]))

(gen-class :name Runner :extends Object :methods [[main [] void]])

(defn- check [condition message]
  (if (not condition)
    (sneaky-throw (RuntimeException. (cast String message)))))

(defn- dictionary []
  [["a" "A" "aa" 0] ["b" "B" "bb" 1]
   ["c" "C" "cc" 0] ["d" "D" "dd" 0]
   ["e" "E" "ee" 0] ["f" "F" "ff" 0]])

(defn- check-question [seed]
  (let [entries (dictionary)
        result (question/generate-question entries seed)
        answers (get result :answers)]
    (check (= "b" (get result :word)) "Zero-weight word selected")
    (check (= "bb" (get result :transcription)) "Wrong transcription")
    (check (= "B" (get result :correct-answer)) "Wrong correct answer")
    (check (= 5 (count answers)) "Expected five answers")
    (check (= 1 (reduce (fn [n answer] (+ n (if (= answer "B") 1 0))) 0 answers))
           "Correct answer must occur once")
    (reduce (fn [seen answer]
              (check (= 1 (reduce (fn [n entry] (+ n (if (= answer (get entry 1)) 1 0))) 0 entries))
                     "Answer outside dictionary")
              (map (fn [previous] (check (not= previous answer) "Repeated answer")) seen)
              (concat seen [answer])) [] answers)
    (question/generate-question entries 321)
    (check (= result (question/generate-question entries seed)) "Seed is not reproducible")
    (check (= entries (dictionary)) "Dictionary was mutated")
    result))

(defn -main [this]
  ;; Seed 42: select B; distractors D A F C; shuffled answers C F D B A.
  (check (= {:word "b" :transcription "bb" :correct-answer "B" :answers ["C" "F" "D" "B" "A"]}
            (question/generate-question (dictionary) 42))
         "Seed must continue between word, distractor and answer selections")
  (let [results (map (fn [seed] (check-question seed)) [0 1 -1 2147483647 -2147483648])]
    (check (> (reduce (fn [n result] (+ n (if (not= result (get results 0)) 1 0))) 0 results) 0)
           "Seed ignored"))
  (check (= [1 5000 0 nil]
            (map (fn [entry] (question/word-weight entry))
                 [["a" "A" "a"] ["b" "B" "b" 5000] ["c" "C" "c" 0] ["d" "D" "d" nil]]))
         "Default or explicit weight lost")
  (map (fn [[entries expected]]
         (check (= expected (get (question/generate-question entries 42) :error))
                "Selection error was not propagated"))
       [[[] :insufficient-items]
        [[["a" "A" "a" -1]] :invalid-weight]
        [[["a" "A" "a" 2147483647] ["b" "B" "b" 1]] :weight-overflow]
        [[["a" "A" "a" 1] ["b" "A" "b" 0] ["c" "C" "c" 0]
          ["d" "D" "d" 0] ["e" "E" "e" 0]] :insufficient-items]])
  nil)
