(ns mogul.test.utils
  (:use clojure.test)
  (:require (mogul [utils :as u])))

;; Map 1=>2, 3=>4 and so on
(def example-map {1 2 3 4 5 6 7 8 9 10})

(deftest inc-map-test
  "Increment entry with key 1 by one, so the map should now
  have an entry with key 1 and value 3"
  (is (= 3 (get (u/inc-map example-map 1) 1))))