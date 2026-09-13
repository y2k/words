(ns words.main-test
  (:require [words.app :as app]))

(defn- click [button world]
  (let [onclick (get-in button [1 :onclick])]
    ((onclick) world)))

(defn- check-language [button-index]
  (let [nodes (atom [])
        dictionary (atom nil)
        world {:update-ui (fn [_w node]
                            (swap! nodes (fn [items] (concat items [node])))
                            nil)
               :clear-ui (fn [_w _arg] (reset! nodes []) nil)
               :shuffle-list (fn [_w items]
                               (if (vector? (get items 0))
                                 (if (= (deref dictionary) nil)
                                   (reset! dictionary items)
                                   (assert (= (deref dictionary) items))))
                               items)}]
    ((app/main) world)
    (assert (= 1 (count (deref nodes))))
    (let [buttons (drop 2 (get-in (deref nodes) [0 2]))]
      (assert (= ["Сербский" "Французский"]
                 (map (fn [button] (get-in button [1 :text])) buttons)))
      (click (get buttons button-index) world))
    (assert (= 1 (count (deref nodes))))
    (assert (>= (count (deref dictionary)) 5))
    ;; Button 0 is "?"; identity shuffle makes button 1 correct and button 2 incorrect.
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
         (assert (= (+ before (if (= answer-index 1) 1 2))
                    (count (deref nodes))))
         (if (not= answer-index 1)
           (assert (= (str "Правильный ответ: " (get entry 1))
                      (get-in (deref nodes) [before 1 :text]))))
         (assert (= (get-in question [2 1 :text])
                    (get-in (deref nodes) [(- (count (deref nodes)) 1) 2 1 :text])))))
     [1 2 0])
    (deref dictionary)))

(defn test []
  (let [serbian (check-language 0)
        french (check-language 1)]
    (assert (not= serbian french))
    (reduce
     (fn [seen entry]
       (assert (= 3 (count entry)))
       (map (fn [field] (assert (not= field nil)) (assert (not= field ""))) entry)
       (let [translation (get entry 1)]
         ;; ponytail: quadratic uniqueness check; use a set if the dictionary becomes large.
         (map (fn [previous] (assert (not= previous translation))) seen)
         (concat seen [translation])))
     []
     french)
    "PASS: language selection, answer transitions, dictionary validation"))
