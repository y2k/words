(ns words.app
  (:require [words.vendor.effect :as effect])
  (:require [words.question :as question])
  (:require [words.dic.serbian :as serbian])
  (:require [words.dic.french :as french]))

(defn- update-ui [view]
  (effect/dispatch :update-ui view))

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

(defn- create-question [dictionary]
  (effect/next
   (effect/dispatch :random-seed nil)
   (fn [seed]
     (let [result (question/generate-question dictionary seed)
           correct-answer (get result :correct-answer)]
       (if (get result :error)
         (update-ui
          [:text {:text "Не удалось выбрать слова: проверьте веса и количество доступных записей."}])
         (update-ui
          (question-view
           (get result :word) (get result :transcription) (get result :answers)
           (fn [answer]
             (if (= answer correct-answer)
               (create-question dictionary)
               (effect/next
                (update-ui [:text {:text (str "Правильно: " correct-answer)
                                  :color "#FF0000"}])
                (fn [_view]
                  (effect/next
                   (update-ui [:divider {}])
                   (fn [_divider]
                     (create-question dictionary))))))))))))))

(defn start [dictionary]
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
