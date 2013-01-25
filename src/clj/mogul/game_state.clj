(ns mogul.game-state
  (:require [clojure.set :as set]
            [mogul.star-map :as star-map]
            [mogul.math :as math]
            [mogul.djikstra :as dj]
            [mogul.utils :as utils]))

;; Eventually create should take at least an id and a list of players

(defn create
  "Build an initial game state!  Currently this only creates the
  locations for stars and edges given the total # of stars in the
  game."
  [total-stars]
  (star-map/create total-stars))

;; static members

;; The maximum tax that a player can charge for shipments
;; traversing an edge
(def max-tax 3)

;; accessors

;; given a game-state and star-id,
;; return a hashset of the star-ids that are connected to star-id via a single edge
(defn neighborhood
  "Find the local neighborhood for a given star id"
  [game-state star-id]
  (disj (reduce set/union
                (map :stars
                     (filter #(contains? (:stars %) star-id)
                             (:edges game-state))))
        star-id))

;; given a game-state and edge-id
;; return edge represented in the game-state with id edge-id or nil if none exists
(defn edge [game-state edge-id]
  (first (filter #(= edge-id (:id %)) (:edges game-state))))

;; given a game-state and player-id
;; return player represented in the game-state with id players-id or nil if none exists
(defn player [game-state player-id]
  (first (filter #(= player-id (:id %)) (:players game-state))))

;; given a game-state and star-id
;; return star represented in the game-state with id star-id or nil if none exists
(defn star [game-state star-id]
  (first (filter #(= star-id (:id %)) (:stars game-state))))

;; given a list of two edges (eg. (:edges game-state)
;; return the edge connecting these two stars
(defn get-edge-from-members [edges star-id-a star-id-b]
  (let [member-set #{star-id-a star-id-b}]
    (first (filter #(= (:stars %) member-set) edges))))

(defn player-ids
  "given a game state
   return list of all player ids"
  [game-state]
  (map :id (:players game-state)))

(defn player-priorities
  "given a game state
   return a hashmap of player ids to player priority"
  [game-state]
  (reduce (fn [m {:keys [id priority]}]
            (assoc m id priority))
          {}
          (:players game-state)))

(defn player-credits
  "given the list of players (eg (:players game-state)
   return a hashmap of player ids to player credits"
  [players]
  (reduce (fn [m {:keys [id credits]}]
            (assoc m id credits))
          {}
          players))

(defn edges-initialized
  "Given a game-state
   Return the edges that have been initialized.
   This means put up for auction at least once If an edge has never
   been put up for auction then it's :owner should be :NA"
   [game-state]
  (let [edges (:edges game-state)]
    (filter (fn [edge] (not (= (:owner edge) :NA)))
            edges)))

(defn stars-initialized
  "Given a game-state
   Return the stars that have been initialized.
   This means that a star is connected to an edge that has been
   auctioned at least once."
  [game-state]
  (let [edges (edges-initialized game-state)]
    (map #(star game-state %)
         (reduce (fn [m edge] (set/union m (:stars edge)))
                 #{}
                 edges))))

(defn sinks-global
  "Given: game state and type (:dilithium)
   return all stars that are sinks of this type"
  [game-state type]
  (filter #(contains? (:sinks %) type) (:stars game-state)))

(defn sources-global
  "Given: game state and type (:dilithium)
   return all stars that are sources of this type"
  [game-state type]
  (filter #(contains? (:sources %) type) (:stars game-state)))

(defn sources
  "Given: game state and type (:dilithium)
   return all stars that are sources of this type that have been initialized"
  [game-state type]
  (map #(star game-state (:id %))
       (set/intersection (set (sources-global game-state type))
                         (set (stars-initialized game-state)))))

(defn sinks
  "Given: game state and type (:dilithium)
   return all stars that are sources of this type that have been initialized"
  [game-state type]
  (map #(star game-state (:id %))
       (set/intersection (set (sinks-global game-state type))
                         (set (stars-initialized game-state)))))

(defn sources-dilithium
  "Given: game state
   return all stars that are sources of :dilithium that have been initialized"
  [game-state]
  (sources game-state :dilithium))

(defn sources-supplies
  "Given: game state
   return all stars that are sources of supplies that have been initialized"
  [game-state]
  (sources game-state :supplies))

(defn sinks-dilithium
  "Given: game state
   return all stars that are sinks of :dilithium that have been initialized"
  [game-state]
  (sinks game-state :dilithium))

(defn sinks-supplies
  "Given: game state
   return all stars that are sinks of supplies that have been initialized"
  [game-state]
  (sinks game-state :supplies))


;; REGION Distance functions
;;
;; Distance functions.  Determine the distance between nodes (stars)
;; in the game-map This can mean either functions used to help
;; determine the cost of shipping resources through a particular edge
;; or the euclidean distance functions that are used to help break
;; ties on shipping

;; Calculate the cost of shipping through a particula edge, combining
;; the inherent cost plus the tax charged from the player owning the
;; edge.
(defn- cost [edge]
  (+ (:cost edge) (:tax edge)))

(defn distance
  "given gamestate src node id and dest node id
   return Distance between two neighbor nodes.  Assumes bidirectional edges.
   If there is no edge containing these two nodes, then max int"
  [game-state src dest]
  (let [edge (get-edge-from-members (:edges game-state) src dest)]
    (if (= nil edge)
      1000000
      (cost edge))))

(defn distance-euclid
  "given game state source star id and dest star id
   get the euclidean distance between two stars"
  [game-state src-id dest-id]
  (let [src (star game-state src-id)
        dest (star game-state dest-id)
        src-x (first (:location src))
        src-y (second (:location src))
        dest-x (first (:location dest))
        dest-y (second (:location dest))]
    (Math/sqrt (+ (math/square (- dest-x src-x)) (math/square (- dest-y src-y))))))

(defn distance-with-euclid
  "given game state source star id and dest star id
   get the distance, but also add a small modifier for the
   euclidean distance between two stars to break ties"
  [game-state src dest]
  (+ (distance game-state src dest)
     (/ (distance-euclid game-state src dest) 1000000)))

(defn shortest-dilithium-routes
  "given a game-state, return a 3-level hashmap where the first level is the dest node,
   the second level is the source node, and the third level is an entry containing both
   the credit cost of the shortest route and a list showing the route of the shortest path.
   In the event of ties, the overall shortest euclidean distance path for the competing paths
   with the same cost is chosen.  The routes returned are in the form:
   {0 {4 {:credits 6, :path (0 2 4)},
       3 {:credits 4, :path (0 3)},
       2 {:credits 3, :path (0 2)},
       1 {:credits 3, :path (0 1)}}}
   Where in this example 0 is a destination of dilithium, 4 is a source of dilithium,
   the shortest path between 4 and 0 is (4 2 0), and it would cost 6 credits to take this
   route."
  [game-state]
  (let [raw-routes (dj/calc-all game-state
                                (map :id (sinks game-state :dilithium))
                                (map :id (sources game-state :dilithium))
                                neighborhood
                                distance-with-euclid)]
    (reduce (fn [m routes]
              (assoc m
                (first routes)
                (dj/pretty-routes (second routes))))
            {}
            raw-routes)))

;; Bids

(defn- until-bid-max
  "given a game-state
   return the largest value of until-bid"
  [game-state]
  (reduce max (map :until-bid (:edges game-state))))

(defn- until-bid-decrement-edge
  "given an edge and the max value for until bid return what the new
   value of the edge will be when decremented.  If it is 0, then
   return max, otherwise return until-bid - 1"
  [edge max]
  (if (>= 0 (:until-bid edge))
    max
    (dec (:until-bid edge))))

(defn- until-bid-decrement-edges
  "given a game state
   return the new edges and decrement all until bid values for all edges
   (helper function for until-bid-decrement-game-state)"
  [game-state]
  (map #(assoc %
          :until-bid
          (until-bid-decrement-edge % (until-bid-max game-state)))
       (:edges game-state)))

(defn- until-bid-decrement-game-state
  "given a game state
   return the game state with its each of its edges' until-bid decremented"
  [game-state]
  (assoc game-state :edges (until-bid-decrement-edges game-state)))

(defn player-bids
  "Given a game-state and an edge-id
   Return a list of bids for this edge.  Example result:
   ({:player 111, :edge 2, :credits 12} {:player 112, :edge 2, :credits 12})"
  [game-state edge-id]
  (remove nil?
          (map (fn [{:keys [player bids]}]
                 (let [bid (first (filter #(= edge-id (:edge %)) bids))]
                   (when bid
                     (assoc bid :player player))))
               (:turns game-state))))

;; Taxes

(defn- edge-tax-helper
  "edge-tax-helper - gets all tax commands for a particular edge"
  [game-state edge-id]
  (remove nil?
          (map (fn [{:keys [player taxes]}]
                 (let [tax (first (filter #(= edge-id (:edge %)) taxes))]
                   (when tax
                     (assoc tax :player player))))
                (:turns game-state))))

(defn edge-tax-command
  "given a game state and an edge-id
   return the tax command for particular edge from owner of that edge"
  [game-state edge-id]
  (first (filter #(= (:owner (edge game-state edge-id)) (:player %))
          (edge-tax-helper game-state edge-id))))

(defn- update-bid-results [results edge-bids]
  (if (empty? edge-bids)
    results
    (let [{:keys [priorities winners]} results
          max-credit (apply max (map :credits edge-bids))
          top-bids (filter #(= (:credits %) max-credit)
                           edge-bids)
          top-bids (map (fn [bid]
                          (assoc bid
                            :priority (get priorities (:player bid))))
                        top-bids)
          top-bids (sort-by :priority > top-bids)
          winner (first top-bids)
          losers (next top-bids)
          winner-id (:player winner)
          loser-ids (map :player losers)
          other-ids (remove #(contains? (into #{} (map :player top-bids)) %)
                            (keys priorities))
          new-priority (apply hash-map
                              (flatten (map list
                                            (concat [winner-id]
                                                    other-ids
                                                    loser-ids)
                                            (range))))]
      (assoc results
        :priorities new-priority
        :winners (conj winners winner)))))

(defn- update-player-priorities [players priorities]
  (map #(assoc % :priority (get priorities (:id %))) players))

(defn- update-player-credits-for-purchase [players winners]
  (let [player-credits (player-credits players)
        player-credits (reduce (fn [m winner]
                                 (assoc m
                                   (:player winner)
                                   (- (get player-credits (:player winner))
                                      (:credits winner))))
                               player-credits
                               winners)]
    (map #(assoc % :credits (get player-credits (:id %))) players)))


(defn bid-results
  "given game state
   return hashmap of priorities and winners eg
   {:priorities {111 0, 112 2, 113 1},
    :winners [{:player 111, :edge 2, :credits 12, :priority 2}]}"
  [game-state]
  (let [edge-ids (sort (map :id (filter #(= 0 (:until-bid %))
                                        (:edges game-state))))]
    (reduce update-bid-results
            {:priorities (player-priorities game-state)
             :winners []}
            (map #(player-bids game-state %) edge-ids))))

(defn- apply-winners
  "given all of the edges for the game-state and the winning bids
   return edges with ownership updated to reflect winning bids"
  [edges winners]
  (let [up-for-bid (filter #(= (:until-bid %) 0) edges)
        edge-winners (reduce (fn [m {:keys [edge player]}]
                               (assoc m edge player))
                             {}
                             winners)]
    (map (fn [edge] (if (= 0 (:until-bid edge))
                      (if (not (nil? (get edge-winners (:id edge))))
                        (assoc edge
                          :owner (get edge-winners (:id edge))
                          :tax max-tax)
                        (dissoc edge :owner))
                      edge))
         edges)))

(defn- apply-taxes [game-state]
  (map (fn [edge]
         (let [tax (edge-tax-command game-state (:id edge))]
           (if tax
             (assoc edge :tax (:credits tax))
             edge)))
       (:edges game-state)))

(defn- get-shipping-fees [game-state]
  (let [djikstras (shortest-dilithium-routes game-state)
        players (:players game-state)
        edges (:edges game-state)
        edge-counts (dj/get-edge-counts
                     djikstras
                     (map :id (sources game-state :dilithium))
                     (map :id (sinks game-state :dilithium)))]
    (reduce (fn [m edge-count]
              ;;(add (get edge cost times edge counts) to player credits)
              (let [members (seq (first edge-count))
                    count (second edge-count)
                    edge (get-edge-from-members edges (first members) (second members))
                    owner-id (:owner edge)
                    owner (player game-state owner-id)
                    ]
                (when owner
                  (assoc m owner-id (+ (if-let [sum (get m owner-id)] sum 0)
                                       (* count (cost edge)))))))
            {}
            edge-counts)))

(defn- update-player-shipping-fees [players shipping-fees]
  (map #(assoc % :credits (+
                           (:credits %)
                           (if-let [fee (get shipping-fees (:id %))]
                             fee
                             0)))
       players))

(defn resolve-turn-helper
  "Resolve turn instructions
    1. who won the star, set owner, decrement cash
    2. resolve non-bids: unset owner
    3. update taxes:
       a. for winners
       b. for changes
    4. Resolve shipping fees: Pay each owner according to the
       cheapest route (solve ties with euclidean dist)
   Bookkeeping
    1. game-state-id gets incremented
    2. time-until bid for each edge gets decremented
    3. clear turns"
  [game-state]
  (let [new-bid-results (bid-results game-state)
        new-players-with-pri (update-player-priorities (:players game-state)
                                                       (:priorities new-bid-results))
        new-players-with-creds (update-player-credits-for-purchase new-players-with-pri
                                                                   (:winners new-bid-results))
        game-state (assoc game-state :players new-players-with-creds)
        edges-with-winners (apply-winners (:edges game-state) (:winners new-bid-results))
        game-state (assoc game-state :edges edges-with-winners)
        edges-with-taxes (apply-taxes game-state)
        game-state (assoc game-state :edges edges-with-taxes)
        player-income (get-shipping-fees game-state)
        new-players-with-income (update-player-shipping-fees (:players game-state) player-income)
        game-state (assoc game-state :players new-players-with-income)
        game-state (utils/inc-map game-state :id)
        game-state (until-bid-decrement-game-state game-state)
        game-state (assoc game-state :turns [])]
    game-state))

(defn resolve-turn [game-state player-turns]
  (let [game-state (assoc game-state :turns player-turns)]
    (resolve-turn-helper game-state)))
