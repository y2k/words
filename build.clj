(ns build (:require [make :as make]))

(deps {:make "0.6.0"})

(make/makefile
 {:target "java"
  :dirs [{:path "src"
          :build-dir ".build/bin/app/src/main/java"}
         {:path "test"
          :build-dir ".build/bin/test/src/main/java"}]})
