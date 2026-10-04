(ns words.question
  (:require [words.sampling :as sampling]))

(defn word-weight [entry]
  (if (= (count entry) 3) 1 (get entry 3)))

(defn generate-question [dictionary seed]
  (let [selected (sampling/sample! dictionary 1 (fn [entry] (word-weight entry)) seed)]
    (if (get selected :error)
      selected
      (let [[word correct-answer transcription] (get-in selected [:items 0])
            wrong (sampling/sample! dictionary 4
                                    (fn [candidate]
                                      (if (= (get candidate 1) correct-answer) 0 1))
                                    (get selected :seed))]
        (if (get wrong :error)
          wrong
          (let [answers (sampling/sample!
                         (concat [correct-answer]
                                 (map (fn [candidate] (get candidate 1)) (get wrong :items)))
                         5 (fn [_answer] 1) (get wrong :seed))]
            (if (get answers :error)
              answers
              {:word word
               :transcription transcription
               :correct-answer correct-answer
               :answers (get answers :items)})))))))
