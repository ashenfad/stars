(ns mogul.test.test-utils
  (:require (mogul [math :as math])))

(defn almost-equal [expected actual]
  (< (Math/abs expected) actual) 0.001)
