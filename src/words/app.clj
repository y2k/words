(ns words.app
  (:require [words.effect :as effect]))

(defn- update-ui [view]
  (effect/dispatch :update-ui view))

(defn- WORDS []
  [["zdravo" "привет" "здра'во"]
   ["hvala" "спасибо" "хва'ла"]
   ["molim" "пожалуйста" "мо'лим"]
   ["da" "да" "да"]
   ["ne" "нет" "не"]
   ["dobro" "хорошо" "до'бро"]
   ["dan" "день" "дан"]
   ["voda" "вода" "во'да"]
   ["ljubav" "любовь" "лю'бав"]
   ["prijatelj" "друг" "прия'тель"]

   ["izvinite" "извините" "изви'ните"]
   ["doviđenja" "до свидания" "дови'дженья"]
   ["dobro jutro" "доброе утро" "до'бро ю'тро"]
   ["dobro veče" "добрый вечер" "до'бро ве'че"]
   ["laku noć" "спокойной ночи" "ла'ку ноч"]

   ["ulaz" "вход" "у'лаз"]
   ["izlaz" "выход" "и'злаз"]
   ["otvoreno" "открыто" "отво'рено"]
   ["zatvoreno" "закрыто" "затво'рено"]
   ["radnja" "магазин" "ра'дня"]

   ["pekara" "пекарня" "пе'кара"]
   ["apoteka" "аптека" "апоте'ка"]
   ["bolnica" "больница" "бо'льница"]
   ["stanica" "остановка" "ста'ница"]
   ["ulica" "улица" "у'лица"]

   ["levo" "налево" "ле'во"]
   ["desno" "направо" "де'сно"]
   ["pravo" "прямо" "пра'во"]
   ["koliko" "сколько" "ко'лико"]
   ["račun" "счет (чек)" "ра'чун"]

   ["kesa" "пакет" "ке'са"]
   ["treba" "нужно" "тре'ба"]
   ["vam" "вам" "вам"]

   ["nula" "ноль" "ну'ла"]
   ["jedan" "один" "е'дан"]
   ["dva" "два" "два"]
   ["tri" "три" "три"]
   ["četiri" "четыре" "че'тири"]
   ["pet" "пять" "пет"]
   ["šest" "шесть" "шест"]
   ["sedam" "семь" "се'дам"]
   ["osam" "восемь" "о'сам"]
   ["devet" "девять" "де'вет"]
   ["deset" "десять" "де'сет"]])

(defn- shuffle [items]
  (effect/dispatch :shuffle-list items))

(defn- question-view [word transcription translations select-answer]
  [:column {}
   [:text {:text (str word " (" transcription ")")}]
   (concat
    [:row {}]
    (map
     (fn [translation]
       [:button {:text translation
                 :onclick (fn [] (select-answer translation))}])
     translations))])

(defn- question-data [words]
  [(get (get words 0) 0)
   (get (get words 0) 2)
   (map (fn [index]
          (get (get words index) 1))
        [0 1 2 3 4])])

(defn- create-question []
  (effect/next
   (shuffle (WORDS))
   (fn [entries]
     (let [[word transcription translations] (question-data entries)
           correct-answer (get translations 0)]
       (effect/next
        (shuffle translations)
        (fn [answers]
          (update-ui
           (question-view
            word
            transcription
            answers
            (fn [answer]
              (if (= answer correct-answer)
                (create-question)
                (effect/next
                 (update-ui
                  [:text {:text (str "Правильный ответ: " correct-answer)}])
                 (fn [_view] (create-question)))))))))))))

(defn main []
  (update-ui
   [:column {}
    [:row {}
     [:button {:text "Start"
               :onclick (fn [] (create-question))}]]]))
