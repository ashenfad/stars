(ns mogul.math)

(defn square [x]
  "Square a number"
  (* x x))

(defn rand-normal
   "Transform a sequence of uniform random number in the interval [0, 1)
    into a sequence of Gaussian random numbers. (by Konrad Hinsen)"
   []
   (let [V1 (dec (* 2.0 (rand)))
         V2 (dec (* 2.0 (rand)))
         S  (+ (* V1 V1) (* V2 V2))
         LS (Math/sqrt (/ (* -2.0 (Math/log S)) S))
         X1 (* V1 LS)]
     (if (or (>= S 1) (= S 0))
       (recur)
       X1)))