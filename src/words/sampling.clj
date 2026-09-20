(ns words.sampling
  (:import [java.util ArrayList]))

(defn- prepare [items weight-fn]
  (let [entries (ArrayList.)]
    (reduce
     (fn [state item]
       (if (get state :error)
         state
         (let [weight (weight-fn item)
               total (get state :total)]
           (cond
             (not (and (instance? Integer weight) (>= weight 0)))
             {:error :invalid-weight}
             (> weight (- 2147483647 total))
             {:error :weight-overflow}
             :else
             (do
               (.add entries [item weight])
               {:entries entries
                :total (+ total weight)
                :positive (+ (get state :positive) (if (> weight 0) 1 0))})))))
     {:entries entries :total 0 :positive 0}
     items)))

(defn- pick-index [entries ticket]
  (get
   (reduce
    (fn [[index remaining selected] entry]
      (let [weight (get entry 1)]
        (if (not= selected nil)
          [(+ index 1) remaining selected]
          (if (< remaining weight)
            [(+ index 1) remaining index]
            [(+ index 1) (- remaining weight) nil]))))
    [0 ticket nil]
    entries)
   2))

(defn sample! [items amount weight-fn random-below]
  (cond
    (not (and (instance? Integer amount) (>= amount 0)))
    {:error :invalid-amount}
    (= amount 0)
    {:items []}
    :else
    (let [prepared (prepare items weight-fn)]
      (cond
        (get prepared :error) prepared
        (> amount (get prepared :positive)) {:error :insufficient-items}
        :else
        (let [entries (cast ArrayList (get prepared :entries))
              result (ArrayList.)]
          ;; ponytail: O(n*k); для больших выборок заменить линейный поиск деревом сумм.
          (reduce
           (fn [total _item]
             (let [index (pick-index entries (random-below total))
                   [item weight] (get entries index)]
               (.add result item)
               (.set entries (cast int index) [item 0])
               (- total weight)))
           (get prepared :total)
           (drop (- (count items) amount) items))
          {:items result})))))
