(ns words.view
  (:import [android.content Context]
           [android.view View ViewGroup]
           [android.widget LinearLayout Button TextView]
           [com.google.android.flexbox FlexboxLayout]))

(defn dp [^Context context ^int value]
  (cast int (android.util.TypedValue/applyDimension
             1 value (.getDisplayMetrics (.getResources context)))))

(defn disable-children! [^ViewGroup parent ^int index]
  (if (< index (.getChildCount parent))
    (let [child (.getChildAt parent index)]
      (.setEnabled child false)
      (if (instance? ViewGroup child)
        (disable-children! child 0))
      (disable-children! parent (+ index 1)))))

(defn- create-button [w {:text title :onclick onclick :compact compact :description description}]
  (let [context (cast Context (get w :context))
        btn (Button. context)
        gap (cast int (dp context 8))
        params (FlexboxLayout.LayoutParams. -2 -2)]
    (.setMargins params 0 0 gap gap)
    (.setLayoutParams btn params)
    (.setText btn (cast String title))
    (if description
      (.setContentDescription btn (cast String description)))
    (if compact
      (let [size (cast int (dp context 48))]
        (.setMinWidth btn size)
        (.setMinimumWidth btn size)
        (.setMinimumHeight btn size)))
    (.setOnClickListener btn
                         ^void:View.OnClickListener
                         (fn [_e]
                           ((onclick) w)))
    btn))

(defn- create-text [{:context ^Context context} {:text ^String title}]
  (let [text (TextView. context)]
    (.setText text title)
    (.setTextSize text 28)
    (.setPadding text 0 0 0 (cast int (dp context 8)))
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
    (.setPadding layout 0 0 0 (cast int (dp context 8)))
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
