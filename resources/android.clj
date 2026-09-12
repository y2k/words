(ns android
  (:require [android-build :as android]))

(deps {:android-build "0.1.0"})

(android/dependencies
 ["com.google.android.flexbox:flexbox:3.0.0"])
