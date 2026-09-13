(ns manifest (:require [xml :as xml]))

(deps {:xml "0.3.0"})

(xml/to-string
 [:manifest {:xmlns:android "http://schemas.android.com/apk/res/android"}
  [:application {:android:icon "@drawable/words_launcher"
                 :android:label "Words"
                 :android:roundIcon "@drawable/words_launcher"
                 :android:theme "@style/WordsTheme"}
   [:activity {:android:name "words.main$MainActivity"
               :android:configChanges "orientation|screenSize"
               :android:exported "true"}
    [:intent-filter {}
     [:action {:android:name "android.intent.action.MAIN"}]
     [:category {:android:name "android.intent.category.LAUNCHER"}]]]]])
