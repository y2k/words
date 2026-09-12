(ns words.effect)

(defn dispatch [fx arg]
  (fn [w] (let [f (get w fx)] (f w arg))))

(defn next [effect f]
  (fn [w]
    (let [value (effect w)
          next-effect (f value)]
      (next-effect w))))
