;; Run make check-ui after make run on an emulator.
(ns checks.disabled_history
  (:import [java.util List]
           [java.util.regex Pattern]
           [java.io StringReader]
           [javax.xml.parsers DocumentBuilderFactory]
           [javax.xml.xpath XPathFactory]
           [org.xml.sax InputSource]
           [org.w3c.dom Document Element NodeList]))

(gen-class
 :name Runner
 :extends Object
 :methods [[main [] void]])

(defn- check [condition message]
  (if (not condition)
    (sneaky-throw (RuntimeException. (cast String message)))))

(defn- adb [args]
  (let [builder (ProcessBuilder. (cast List (concat ["adb"] args)))
        process (.start (.redirectErrorStream builder true))
        output (String. (.readAllBytes (.getInputStream process)) "UTF-8")
        status (.waitFor process)]
    (check (= status 0) (str "adb " args " failed: " output))
    output))

(defn- layout []
  (adb ["shell" "uiautomator" "dump" "/sdcard/words-check.xml"])
  (let [xml (adb ["exec-out" "cat" "/sdcard/words-check.xml"])
        factory (DocumentBuilderFactory/newInstance)]
    (.setFeature factory "http://apache.org/xml/features/disallow-doctype-decl" true)
    (.parse (.newDocumentBuilder factory)
            (InputSource. (StringReader. (cast String xml))))))

(defn- query [document expression]
  (.evaluate (.newXPath (XPathFactory/newInstance))
             (cast String expression) (cast Object document)))

(defn- coordinate [^java.util.regex.Matcher matcher]
  (check (.find matcher) "Invalid button bounds")
  (Integer/parseInt (.group matcher)))

(defn- tap [document selector]
  (let [bounds (cast String (query document (str "(" selector ")[1]/@bounds")))
        matcher (.matcher (Pattern/compile "[0-9]+") bounds)]
    (check (not (.isEmpty bounds)) (str "Missing element: " selector))
    (let [x1 (coordinate matcher)
          y1 (coordinate matcher)
          x2 (coordinate matcher)
          y2 (coordinate matcher)]
      (adb ["shell" "input" "tap" (str (/ (+ x1 x2) 2)) (str (/ (+ y1 y2) 2))]))))

(defn- snapshot-nodes [^NodeList nodes ^int index]
  (if (< index (.getLength nodes))
    (let [node (cast Element (.item nodes index))]
      (concat
       (if (= (.getAttribute node "package") "im.y2k.fixme")
         [[(.getAttribute node "class")
           (.getAttribute node "text")
           (.getAttribute node "content-desc")
           (.getAttribute node "enabled")]]
         [])
       (snapshot-nodes nodes (+ index 1))))
    []))

(defn- snapshot [^Document document]
  (snapshot-nodes (.getElementsByTagName document "node") 0))

(defn- check-buttons [document]
  (check (= (query document "count(//node[@class='android.widget.Button' and @enabled='true'])") "6")
         "Only the new question must have six active buttons")
  (check (= (query document "count(//node[@class='android.widget.Button' and @enabled='false'])") "6")
         "All six old answers must be disabled")
  (check (= (query document "count(//node[@text='?' and @enabled='false'])") "1")
         "The old unknown-answer button must be disabled"))

(defn -main [this]
  (adb ["shell" "am" "force-stop" "im.y2k.fixme"])
  (adb ["shell" "am" "start" "-W" "-n" "'im.y2k.fixme/words.main$MainActivity'"])
  (tap (layout) "//node[@text='СЕРБСКИЙ' or @text='Сербский']")
  (tap (layout) "//node[@text='?']")
  (let [after (layout)]
    (check-buttons after)
    (tap after "//node[@class='android.widget.Button' and @enabled='false']")
    (check (= (snapshot (layout)) (snapshot after)) "Disabled answer changed the history")
    (tap after "//node[@text='?' and @enabled='true']")
    (let [next (layout)]
      (check (= (query next "count(//node[@text='?' and @enabled='true'])") "1")
             "The new question must be active")
      (check (= (query next "count(//node[@text='?' and @enabled='false'])") "2")
             "Answering must append a question and disable the previous one")))
  "PASS: old answers disabled, old taps ignored, new question active")
