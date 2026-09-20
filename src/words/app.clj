(ns words.app
  (:require [words.effect :as effect])
  (:require [words.dic.serbian :as serbian])
  (:require [words.dic.french :as french]))

(defn- update-ui [view]
  (effect/dispatch :update-ui view))

(defn- sample [items amount weight-fn]
  (effect/dispatch :sample [items amount weight-fn]))

(defn- with-sample [request continuation]
  (effect/next
   request
   (fn [result]
     (if (get result :error)
       (update-ui
        [:text {:text "Не удалось выбрать слова: проверьте веса и количество доступных записей."}])
       (continuation (get result :items))))))

(defn- question-view [word transcription translations select-answer]
  [:column {}
   [:text {:text (str word " (" transcription ")")}]
   (concat
    [:row {}
     [:button {:text "?"
               :compact true
               :description "Не знаю"
               :onclick (fn [] (select-answer nil))}]]
    (map
     (fn [translation]
       [:button {:text translation
                 :onclick (fn [] (select-answer translation))}])
     translations))])

(defn word-weight [entry]
  (if (= (count entry) 3) 1000 (get entry 3)))

(defn- create-question [dictionary]
  (with-sample
   (sample dictionary 1 (fn [entry] (word-weight entry)))
   (fn [selected]
     (let [[word correct-answer transcription] (get selected 0)]
       (with-sample
        (sample dictionary 4
                (fn [candidate]
                  (if (= (get candidate 1) correct-answer) 0 1)))
        (fn [wrong]
          (with-sample
           (sample (concat [correct-answer]
                           (map (fn [candidate] (get candidate 1)) wrong))
                   5 (fn [_answer] 1))
           (fn [answers]
             (update-ui
              (question-view
               word transcription answers
               (fn [answer]
                 (if (= answer correct-answer)
                   (create-question dictionary)
                   (effect/next
                    (update-ui
                     [:text {:text (str "Правильный ответ: " correct-answer)}])
                    (fn [_view]
                      (effect/next
                       (update-ui [:divider {}])
                       (fn [_divider]
                         (create-question dictionary)))))))))))))))))

(defn- start [dictionary]
  (effect/next
   (effect/dispatch :clear-ui nil)
   (fn [_view]
     (if (and (>= (count dictionary) 5)
              (reduce (fn [valid entry]
                        (and valid (vector? entry)
                             (or (= (count entry) 3) (= (count entry) 4))))
                      true dictionary))
       (create-question dictionary)
       (update-ui
        [:text {:text "Проверьте словарь: нужны минимум 5 записей из слова, перевода, транскрипции и необязательного веса."}])))))

(defn main []
  (update-ui
   [:column {}
    [:row {}
     [:button {:text "Сербский"
               :onclick (fn [] (start (serbian/words)))}]
     [:button {:text "Французский"
               :onclick (fn [] (start (french/words)))}]]]))
