(ns manifest (:require [xml :as xml]))

(deps {:xml "0.3.0"})

(xml/to-string
 [:manifest {:xmlns:android "http://schemas.android.com/apk/res/android"}
  [:application {:android:icon "@drawable/ic_launcher"
                 :android:label "Words"
                 :android:roundIcon "@drawable/ic_launcher"
                 :android:theme "@android:style/Theme.Material.NoActionBar"}
   [:activity {:android:name "words.main$MainActivity"
               :android:configChanges "orientation|screenSize"
               :android:exported "true"}
    [:intent-filter {}
     [:action {:android:name "android.intent.action.MAIN"}]
     [:category {:android:name "android.intent.category.LAUNCHER"}]]]]])
