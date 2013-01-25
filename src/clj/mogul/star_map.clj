(ns mogul.star-map
  (:require [mogul.math :as math]))

(def ^:private game-size 800)
(def ^:private cluster-dev 400)
(def ^:private star-dev 100)
(def ^:private star-separation 70)

(defn- create-cluster []
  (let [star-count (+ 3 (rand-int 4))
        [center-x center-y] (repeatedly 2 #(* cluster-dev (math/rand-normal)))]
    (repeatedly star-count
                (fn [] [(+ center-x (* star-dev (math/rand-normal)))
                        (+ center-y (* star-dev (math/rand-normal)))]))))

(defn- clustered-seq []
  (lazy-seq (concat (create-cluster) (clustered-seq))))

(defn- star-loc-seq []
  (concat [[0 0]] (clustered-seq)))

(defn- distance [loc1 loc2]
  (let [[x1 y1] loc1
        [x2 y2] loc2]
    (Math/sqrt (+ (Math/pow (- x1 x2) 2) (Math/pow (- y1 y2) 2)))))

(defn- within? [range loc1 loc2]
  (>= range (distance loc1 loc2)))

(defn- filtered-locs [locations separator]
  (let [step (fn step [xs seen]
               (lazy-seq
                ((fn [[f :as xs] seen]
                   (when-let [s (seq xs)]
                     (if (and (within? game-size [0 0] f)
                              (not-any? #(within? separator f %) seen))
                       (cons f (step (rest s) (conj seen f)))
                       (recur (rest s) seen))))
                 xs seen)))]
    (step locations #{})))

(defn- sorted-edges [stars]
  (sort-by #(apply distance (map :location %))
           (remove nil? (distinct (for [s1 stars s2 stars]
                                    (when (not= s1 s2) [s1 s2]))))))

(defn- grow-mst [params]
  (let [{:keys [mst connected unconnected edges]} params
        edge-ids (map :id (some (fn [[s1 s2]]
                                  (if (or (and (connected (:id s1))
                                               (unconnected (:id s2)))
                                          (and (connected (:id s2))
                                               (unconnected (:id s1))))
                                    #{s1 s2}))
                                edges))]
    {:mst (conj mst edge-ids)
     :connected (apply conj connected edge-ids)
     :unconnected (apply disj unconnected edge-ids)
     :edges edges}))

(defn- mst [stars]
  (let [edges (sorted-edges stars)
        start-star (:id (ffirst edges))
        init-params {:mst []
                     :connected (conj #{} start-star)
                     :unconnected (disj (into #{} (map :id (flatten edges)))
                                        start-star)
                     :edges edges}]
    (:mst (first (drop-while (comp seq :unconnected)
                             (iterate grow-mst init-params))))))

(defn create
  "Create stars and edges for a game given the total # of stars"
  [star-count]
  (let [stars (map (fn [id loc] {:id id :location loc :seed (rand-int 1000)})
                   (range star-count)
                   (take star-count (filtered-locs (star-loc-seq)
                                                   star-separation)))
        mst-count (Math/floor (* 0.8 star-count))
        full-mst (mst stars)
        partial-msts (repeatedly 4 #(mst (take mst-count
                                               (sort-by (fn [_] (rand)) stars))))
        edge-ids (distinct (apply concat full-mst partial-msts))
        edges (map (fn [id star-ids] {:id id
                                      :owner :NA
                                      :stars (into #{} star-ids)})
                   (range (count edge-ids))
                   edge-ids)]
    {:stars stars
     :edges edges}))
