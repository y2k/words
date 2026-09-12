(ns words.view
  (:import [android.content Context]
           [android.view View ViewGroup]
           [android.widget LinearLayout Button TextView]
           [com.google.android.flexbox FlexboxLayout]))

(defn- create-button [w {:text title :onclick onclick}]
  (let [context (cast Context (get w :context))
        btn (Button. context)]
    (.setText btn (cast String title))
    (.setOnClickListener btn
                         ^void:View.OnClickListener
                         (fn [_e]
                           ((onclick) w)))
    btn))

(defn- create-text [{:context ^Context context} {:text ^String title}]
  (let [text (TextView. context)]
    (.setText text title)
    (.setTextSize text 28)
    text))

(defn- add-children [w ^ViewGroup layout children]
  (reduce (fn [result child]
            (let [f (create child)]
              (.addView layout (cast View (f w)))
              result))
          layout
          children))

(defn- create-layout [w ^int orientation children]
  (let [context (cast Context (get w :context))
        layout (LinearLayout. context)]
    (.setOrientation layout orientation)
    (add-children w layout children)))

(defn- create-row [w children]
  (let [context (cast Context (get w :context))
        layout (FlexboxLayout. context)]
    ;; ly2k has no Java static-field expression: 0 is ROW and 1 is WRAP.
    (.setFlexDirection layout 0)
    (.setFlexWrap layout 1)
    (add-children w layout children)))

(defn create [node]
  (fn [w]
    (let [[tag attrs] node
          children (drop 2 node)]
      (case tag
        :button (create-button w attrs)
        :text (create-text w attrs)
        :column (create-layout w 1 children)
        :row (create-row w children)
        (sneaky-throw (RuntimeException. (str "unknown view: " tag)))))))
