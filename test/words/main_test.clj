(ns words.main-test
  (:require [words.app :as app])
  (:require [words.question :as question])
  (:require [words.dic.serbian :as serbian])
  (:require [words.dic.french :as french]))

(gen-class :name Runner :extends Object :methods [[main [] void]])

(defn- check [condition message]
  (if (not condition)
    (sneaky-throw (RuntimeException. (cast String message)))))

(defn- click [button world]
  (let [onclick (get-in button [1 :onclick])]
    ((onclick) world)))

(defn- check-language [button-index]
  (let [nodes (atom [])
        seeds (atom 0)
        dictionary (if (= button-index 0) (serbian/words) (french/words))
        world {:update-ui (fn [_w node]
                            (swap! nodes (fn [items] (concat items [node]))) nil)
               :clear-ui (fn [_w _arg] (reset! nodes []) nil)
               :random-seed (fn [_w _arg]
                              (swap! seeds (fn [n] (+ n 1))) 42)}]
    ((app/main) world)
    (check (= 0 (deref seeds)) "Menu must not request seed")
    (let [language-buttons (drop 2 (get-in (deref nodes) [0 2]))]
      (check (= ["Сербский" "Французский"]
                (map (fn [button] (get-in button [1 :text])) language-buttons)) "Language menu")
      (click (get language-buttons button-index) world))
    (check (= 1 (count (deref nodes))) "Menu was not cleared")
    (check (= 1 (deref seeds)) "One seed per question")
    (map
     (fn [answer-kind]
       (let [before (count (deref nodes))
             seed-count (deref seeds)
             node (get (deref nodes) (- before 1))
             title (get-in node [2 1 :text])
             correct (reduce (fn [found entry]
                               (if (= title (str (get entry 0) " (" (get entry 2) ")"))
                                 (get entry 1) found)) nil dictionary)
             buttons (drop 2 (get node 3))
             answers (drop 1 buttons)
             answer-texts (map (fn [button] (get-in button [1 :text])) answers)
             chosen (if (= answer-kind :unknown)
                      (get buttons 0)
                      (reduce (fn [found button]
                                (if (= (= (get-in button [1 :text]) correct)
                                       (= answer-kind :correct)) button found)) nil answers))]
         (check (not= nil correct) "Question outside selected dictionary")
         (check (= 5 (count answers)) "Expected five answer buttons")
         (check (= 1 (reduce (fn [n text] (+ n (if (= text correct) 1 0))) 0 answer-texts))
                "Correct answer missing or duplicated")
         (check (= "Не знаю" (get-in buttons [0 1 :description])) "Unknown answer accessibility")
         (click chosen world)
         (check (= (+ seed-count 1) (deref seeds)) "Next question must request exactly one seed")
         (check (= (+ before (if (= answer-kind :correct) 1 3)) (count (deref nodes)))
                "Answer transition")
         (if (not= answer-kind :correct)
           (do
             (check (= (str "Правильно: " correct) (get-in (deref nodes) [before 1 :text]))
                    "Incorrect answer feedback")
             (check (= "#FF0000" (get-in (deref nodes) [before 1 :color]))
                    "Incorrect and unknown answer feedback must be red")
             (check (= :divider (get-in (deref nodes) [(+ before 1) 0])) "Missing divider")))
         (let [next-node (get (deref nodes) (- (count (deref nodes)) 1))]
           (check (= nil (get-in node [2 1 :color])) "Current question must use theme color")
           (check (= nil (get-in next-node [2 1 :color])) "Next question must use theme color")
           (check (= title (get-in next-node [2 1 :text])) "Fixed seed changed word")
           (check (= answer-texts
                     (map (fn [button] (get-in button [1 :text])) (drop 3 (get next-node 3))))
                  "Fixed seed changed answer order"))))
     [:correct :wrong :unknown])))

(defn- check-start-error [dictionary expected seed-count]
  (let [nodes (atom [])
        seeds (atom 0)
        world {:clear-ui (fn [_w _arg] (reset! nodes []) nil)
               :update-ui (fn [_w node] (swap! nodes (fn [items] (concat items [node]))) nil)
               :random-seed (fn [_w _arg] (swap! seeds (fn [n] (+ n 1))) 42)}]
    ((app/start dictionary) world)
    (check (= seed-count (deref seeds)) "Invalid dictionary seed requests")
    (check (= 1 (count (deref nodes))) "Expected one error message")
    (check (= expected (get-in (deref nodes) [0 1 :text])) "Wrong error feedback")
    (check (= nil (get-in (deref nodes) [0 1 :color]))
           "Technical error must keep its existing color")))

(defn- check-seed-forwarding []
  (let [nodes (atom [])
        seed (atom 0)
        dictionary (serbian/words)
        world {:clear-ui (fn [_w _arg] (reset! nodes []) nil)
               :update-ui (fn [_w node] (swap! nodes (fn [items] (concat items [node]))) nil)
               :random-seed (fn [_w _arg] (deref seed))}]
    ((app/start dictionary) world)
    (map (fn [value]
           (reset! seed value)
           (let [previous (get (deref nodes) (- (count (deref nodes)) 1))]
             (click (get-in previous [3 2]) world))
           (let [node (get (deref nodes) (- (count (deref nodes)) 1))
                 expected (question/generate-question dictionary value)]
             (check (= (str (get expected :word) " (" (get expected :transcription) ")")
                       (get-in node [2 1 :text])) "App did not forward supplied seed")
             (check (= (get expected :answers)
                       (map (fn [button] (get-in button [1 :text])) (drop 3 (get node 3))))
                    "App did not forward seed to all selections")))
         [1 -1 2147483647 -2147483648])))

(defn- check-dictionary [dictionary]
  (reduce (fn [seen entry]
            (check (or (= 3 (count entry)) (= 4 (count entry))) "Dictionary entry shape")
            (map (fn [index]
                   (check (and (not= nil (get entry index)) (not= "" (get entry index)))
                          "Empty dictionary field")) [0 1 2])
            (check (or (= 3 (count entry))
                       (and (instance? Integer (get entry 3)) (>= (get entry 3) 0)))
                   "Invalid dictionary weight")
            (map (fn [previous] (check (not= previous (get entry 1)) "Duplicate translation")) seen)
            (concat seen [(get entry 1)])) [] dictionary))

(defn -main [this]
  (check-language 0)
  (check-language 1)
  (check-seed-forwarding)
  (let [normal [["a" "A" "a"] ["b" "B" "b"] ["c" "C" "c"] ["d" "D" "d"] ["e" "E" "e"]]]
    (map (fn [dictionary]
           (check-start-error dictionary
                              "Проверьте словарь: нужны минимум 5 записей из слова, перевода, транскрипции и необязательного веса." 0))
         [[] (drop 1 normal) (concat [(cast Object nil)] normal) (concat [["short" "entry"]] normal)
          (concat [["a" "A" "a" 1 2]] normal)])
    (map (fn [dictionary]
           (check-start-error dictionary
                              "Не удалось выбрать слова: проверьте веса и количество доступных записей." 1))
         [(map (fn [entry] (concat entry [0])) normal)
          (concat [["bad" "BAD" "bad" -1]] normal)
          (map (fn [entry] [(get entry 0) "same" (get entry 2)]) normal)]))
  (check-dictionary (serbian/words))
  (check-dictionary (french/words))
  nil)
