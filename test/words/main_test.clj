(ns words.main-test
  (:require [words.app :as app])
  (:require [words.dic.serbian :as serbian])
  (:require [words.dic.french :as french]))

(defn- click [button world]
  (let [onclick (get-in button [1 :onclick])]
    ((onclick) world)))

(defn- sample-first [items amount weight-fn]
  (case amount
    1 {:items [(get items 0)]}
    4 (do
        (assert (= 0 (weight-fn (get items 0))))
        (map (fn [entry] (assert (= 1 (weight-fn entry)))) (drop 1 items))
        {:items (map (fn [index] (get items index)) [1 2 3 4])})
    5 (do
        (assert (= [1 1 1 1 1] (map weight-fn items)))
        {:items items})))

(defn- check-language [button-index]
  (let [nodes (atom [])
        dictionary (atom (if (= button-index 0)
                           (serbian/words)
                           (french/words)))
        world {:update-ui (fn [_w node]
                            (swap! nodes (fn [items] (concat items [node])))
                            nil)
               :clear-ui (fn [_w _arg] (reset! nodes []) nil)
               :sample (fn [_w [items amount weight-fn]]
                         (sample-first items amount weight-fn))}]
    ((app/main) world)
    (assert (= 1 (count (deref nodes))))
    (let [buttons (drop 2 (get-in (deref nodes) [0 2]))]
      (assert (= ["Сербский" "Французский"]
                 (map (fn [button] (get-in button [1 :text])) buttons)))
      (click (get buttons button-index) world))
    (assert (= 1 (count (deref nodes))))
    (assert (>= (count (deref dictionary)) 5))
    ;; Button 0 is "?"; the deterministic sample puts the correct answer first.
    (map
     (fn [answer-index]
       (let [before (count (deref nodes))
             question (get (deref nodes) (- before 1))
             answers (drop 2 (get question 3))
             entry (get (deref dictionary) 0)]
         (assert (= (str (get entry 0) " (" (get entry 2) ")")
                    (get-in question [2 1 :text])))
         (assert (= (concat ["?"] (map (fn [index] (get-in (deref dictionary) [index 1])) [0 1 2 3 4]))
                    (map (fn [button] (get-in button [1 :text])) answers)))
         (assert (= "Не знаю" (get-in answers [0 1 :description])))
         (click (get answers answer-index) world)
         (assert (= (+ before (if (= answer-index 1) 1 3))
                    (count (deref nodes))))
         (if (not= answer-index 1)
           (do
             (assert (= (str "Правильный ответ: " (get entry 1))
                        (get-in (deref nodes) [before 1 :text])))
             (assert (= :divider (get-in (deref nodes) [(+ before 1) 0])))))
         (assert (= (get-in question [2 1 :text])
                    (get-in (deref nodes) [(- (count (deref nodes)) 1) 2 1 :text])))))
     [1 2 0])
    (deref dictionary)))

(defn- check-weights []
  (assert (= [1000 5000 0 nil]
             (map (fn [entry] (app/word-weight entry))
                  [["a" "A" "a"] ["b" "B" "b" 5000]
                   ["c" "C" "c" 0] ["d" "D" "d" nil]]))))

(defn- check-weighted-question []
  (let [nodes (atom [])
        requests (atom [])
        dictionary [["a" "A" "a" 0] ["b" "B" "b" 1]
                    ["c" "C" "c" 0] ["d" "D" "d" 0]
                    ["e" "E" "e" 0] ["f" "F" "f" 0]]
        world {:update-ui (fn [_w node]
                            (swap! nodes (fn [items] (concat items [node]))) nil)
               :clear-ui (fn [_w _arg] (reset! nodes []) nil)
               :sample
               (fn [_w [items amount weight-fn]]
                 (swap! requests (fn [seen] (concat seen [amount])))
                 (case amount
                   1 (do
                       (assert (= dictionary items))
                       (assert (= [0 1 0 0 0 0] (map weight-fn items)))
                       {:items [(get items 1)]})
                   4 (do
                       (assert (= dictionary items))
                       (assert (= [1 0 1 1 1 1] (map weight-fn items)))
                       {:items (map (fn [index] (get items index)) [0 2 3 4])})
                   5 (do
                       (assert (= ["B" "A" "C" "D" "E"] items))
                       (assert (= [1 1 1 1 1] (map weight-fn items)))
                       {:items (map (fn [index] (get items index)) [4 3 2 1 0])})))}]
    ((app/start dictionary) world)
    (map
     (fn [answer-index]
       (let [before (count (deref nodes))
             question (get (deref nodes) (- before 1))
             answers (drop 2 (get question 3))]
         (assert (= "b (b)" (get-in question [2 1 :text])))
         (assert (= ["?" "E" "D" "C" "A" "B"]
                    (map (fn [button] (get-in button [1 :text])) answers)))
         (click (get answers answer-index) world)
         (assert (= (+ before (if (= answer-index 5) 1 3))
                    (count (deref nodes))))
         (if (not= answer-index 5)
           (do
             (assert (= "Правильный ответ: B"
                        (get-in (deref nodes) [before 1 :text])))
             (assert (= :divider
                        (get-in (deref nodes) [(+ before 1) 0])))))
         (assert (= "b (b)"
                    (get-in (deref nodes)
                            [(- (count (deref nodes)) 1) 2 1 :text])))))
     [5 1 0])
    (assert (= [1 4 5 1 4 5 1 4 5 1 4 5] (deref requests)))))

(defn- check-invalid-start [dictionary]
  (let [nodes (atom [])
        world {:clear-ui (fn [_w _arg] (reset! nodes []) nil)
               :update-ui (fn [_w node]
                            (swap! nodes (fn [items] (concat items [node]))) nil)
               :sample (fn [_w _request] (assert false))}]
    ((app/start dictionary) world)
    (assert (= 1 (count (deref nodes))))
    (assert (= "Проверьте словарь: нужны минимум 5 записей из слова, перевода, транскрипции и необязательного веса."
               (get-in (deref nodes) [0 1 :text])))))

(defn- check-sample-errors []
  (map
   (fn [[failed-amount expected-requests]]
     (let [nodes (atom [])
           requests (atom [])
           dictionary [["a" "A" "a"] ["b" "B" "b"] ["c" "C" "c"]
                       ["d" "D" "d"] ["e" "E" "e"]]
           world {:clear-ui (fn [_w _arg] (reset! nodes []) nil)
                  :update-ui (fn [_w node]
                               (swap! nodes (fn [items] (concat items [node]))) nil)
                  :sample (fn [_w [items amount weight-fn]]
                            (swap! requests (fn [seen] (concat seen [amount])))
                            (if (= failed-amount amount)
                              {:error :invalid-weight}
                              (sample-first items amount weight-fn)))}]
       ((app/start dictionary) world)
       (assert (= expected-requests (deref requests)))
       (assert (= 1 (count (deref nodes))))
       (assert (= "Не удалось выбрать слова: проверьте веса и количество доступных записей."
                  (get-in (deref nodes) [0 1 :text])))))
   [[1 [1]] [4 [1 4]] [5 [1 4 5]]]))

(defn- check-dictionary [dictionary]
  (reduce
   (fn [seen entry]
     (assert (or (= 3 (count entry)) (= 4 (count entry))))
     (map (fn [index]
            (let [field (get entry index)]
              (assert (not= field nil))
              (assert (not= field ""))))
          [0 1 2])
    (assert (not= nil (app/word-weight entry)))
    (assert (>= (app/word-weight entry) 0))
     (let [translation (get entry 1)]
       ;; ponytail: quadratic uniqueness check; use a set if the dictionary becomes large.
       (map (fn [previous] (assert (not= previous translation))) seen)
       (concat seen [translation])))
   []
   dictionary))

(defn test []
  (check-weights)
  (check-weighted-question)
  (check-sample-errors)
  (let [normal [["a" "A" "a"] ["b" "B" "b"] ["c" "C" "c"]
                ["d" "D" "d"] ["e" "E" "e"]]]
    (check-invalid-start [])
    (check-invalid-start (drop 1 normal))
    (map (fn [bad-entry] (check-invalid-start (concat [bad-entry] normal)))
         [nil ["short" "entry"] ["a" "A" "a" 1 2]]))
  (let [serbian (check-language 0)
        french (check-language 1)]
    (assert (not= serbian french))
    (check-dictionary serbian)
    (check-dictionary french)
    "PASS: language selection, answer transitions, sample policies, errors, dictionary validation"))
