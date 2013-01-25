(ns mogul.utils)

(defn inc-map [m key]
  "Given a map and a key, where the value is an integer, increment
   the value and return the updated map"
  (assoc m key (inc (get m key 0))))

(defn safe-shuffle
  "Shuffles a collection and returns a vector."
  [coll]
  (let [coll (vec coll)]
    (first (nth (iterate (fn [[ret candidates]]
                           (let [idx (rand-int (count candidates))]
                             [(conj ret (candidates idx))
                              (subvec (assoc candidates idx (candidates 0))
                                      1)]))
                         [[] coll])
                (count coll)))))
