(ns mogul.test.math
  (:use clojure.test)
  (:require (mogul [math :as math])
            (mogul.test [test-utils :as test-utils])))

(deftest square-test
  "16**2 = 256"
  (is (= 256 (math/square 16))))
