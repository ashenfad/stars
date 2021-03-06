; DO NOT EDIT THIS FILE! IT WAS AUTOMATICALLY GENERATED BY
; lein-cljsbuild FROM THE FOLLOWING SOURCE FILE:
; file:/Users/ashenfad/git/stars/src/clj/mogul/game_gen.clj

(ns mogul.game-gen
  (:require [mogul.star-map :as star-map]
            [mogul.utils :as utils]
            [clojure.set :as set]))

(def ^:private ai-names
  ["Hal" "Daneel" "Robbie" "Bishop" "Marvin" "Blue" "Watson"])

(def ^:private star-names
 ["Algol" "Altair" "Alya" "Andromeda" "Anteres" "Antila" "Apus"
  "Aquarius" "Ara" "Arctutus" "Aries" "Arrakis" "Atlas" "Auriga"
  "Azha" "Bied" "Bootes" "Caelum" "Cancer" "Canis" "Castor" "Carina"
  "Cassiopeia" "Centaurus" "Cetus" "Circinus" "Corona" "Corvus"
  "Crux" "Cygnus" "Delphinus" "Deneb" "Dorado" "Draco" "Electra"
  "Equuleus" "Fornax" "Gienah" "Gemini" "Grus" "Hadar" "Hercules"
  "Hydra" "Indus" "Jabbah" "Kied" "Kochab" "Kuma" "Lesath" "Leo"
  "Libra" "Lupus" "Lynx" "Lyra" "Mensa" "Merope" "Mira" "Mizar"
  "Musca" "Nashira" "Niyat" "Norma" "Octans" "Orion" "Pavo"
  "Pegasus" "Perseus" "Phoenix" "Pisces" "Polaris" "Pollux"
  "Procyon" "Puppis" "Pyxis" "Rana" "Regulus" "Rigel" "Sadr"
  "Sagittarius" "Sargas" "Scorpius" "Sirius" "Tarazed" "Tarus"
  "Tejat" "Tucana" "Tyl" "Ursa" "Vega" "Vela" "Virgo" "Volans"
  "Wezn" "Ukdah" "Zozma"])

(def ^:private player-name "Player")
(def ^:private sol-name "Sol")

(defn- sol? [star]
  (= 0 (:id star)))

(defn- gen-player [id type name priority]
  {:id id
   :type type
   :name name
   :priority priority
   :credits 10})

(defn- gen-players [ai-players]
  (map gen-player
       (range  ai-players)
       (repeat :ai-random)
       (utils/safe-shuffle ai-names)
       (reverse (range ai-players))))

;; (gen-player 0 :human player-name ai-players)

(defn- assign-edge-cost [edges]
  (map #(assoc % :tax 3 :cost 1)
       edges))

(defn- assign-bid-order [edges bids-per-turn]
  (let [turns (keep :until-bid edges)
        next-turn (if (seq turns)
                    (inc (apply max turns))
                    0)
        connected (reduce set/union
                          #{0}
                          (map :stars (filter :until-bid edges)))
        {:keys [fringe other]} (group-by (fn [{:keys [stars until-bid]}]
                                           (let [[s1 s2] (seq stars)]
                                             (if (or (and (not (connected s1))
                                                          (not (connected s2)))
                                                     (not (nil? until-bid)))
                                               :other
                                               :fringe)))
                                         edges)
        updated (concat (map #(assoc % :until-bid next-turn)
                             (take bids-per-turn fringe))
                        (drop bids-per-turn fringe)
                        other)]
    (if (seq (remove :until-bid updated))
      (assign-bid-order updated bids-per-turn)
      updated)))

(defn- assign-resources [stars]
  (map (fn [star]
         (if (sol? star)
           (assoc star
             :sources {}
             :sinks {:dilithium 999})
           (assoc star
             :sources {:dilithium 1}
             :sinks {})))
       stars))

(defn- assign-star-names [stars]
  (map (fn [star name]
         (assoc star :name (if (sol? star) sol-name name)))
       stars
       (cycle (utils/safe-shuffle star-names))))

(defn create
  "Constructs an initial game state with a single human player.

  Optional parameters
    :ai-players - The number of ai-players.
    :stars - The number of stars.
    :bids-per-turn - The number of bids allowed per turn."
  [& {:keys [ai-players stars bids-per-turn]
      :or {ai-players 3 stars 15 bids-per-turn 4}}]
  (let [{:keys [stars edges]} (star-map/create stars)]
    {:players (gen-players ai-players)
     :stars (assign-star-names (assign-resources stars))
     :edges (assign-edge-cost (assign-bid-order edges bids-per-turn))}))
