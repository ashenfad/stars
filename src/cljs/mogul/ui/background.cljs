(ns mogul.ui.background
  (:require [mogul.ui.canvas :as canvas]
            [mogul.ui.transform :as transform]))

(defn create
  ([]
     (create 512 16 1))
  ([tile-size tile-stars weight]
     {:weight weight
      :tile-size tile-size
      :tile-stars tile-stars
      :seed (rand)
      :transform (transform/scale (transform/create) weight weight)}))

(defn- draw-tile! [transform context seed tile-x tile-y tile-size tile-stars]
  (let [id (str seed "-" tile-x "-" tile-y)
        x-offset (* tile-x tile-size)
        y-offset (* tile-y tile-size)
        all-pts ((. (js/Alea. id) -block) tile-stars tile-size tile-size)
        pairs (map list
                   (partition-all (/ tile-stars 3 ) all-pts)
                   ["rgba(255, 255, 255, 0.4)"
                    "rgba(255, 255, 255, 0.6)"
                    "rgba(255, 255, 255, 1.0)"])]
    (doseq [[pts color] pairs]
      (canvas/fill-style! context color)
      (doseq [[x y] pts]
        (let [[tx ty] (transform/transform-point transform (+ x x-offset) (+ y y-offset))]
          (canvas/fill-rect! context tx ty 1 1))))))

(defn- tile-index [val tile-size]
  (Math/floor (/ val tile-size)))

(defn- scale [sm scale x y]
  (let [{:keys [weight transform]} sm
        adj (transform/translate (transform/create) x y)
        inv-weight (/ 1 weight)
        scale (if (> scale 1)
                (+ 1 (* inv-weight (- scale 1)))
                (/ 1 (+ 1 (* inv-weight (- (/ 1 scale) 1)))))]
    (assoc sm :transform (transform/multiply (transform/scale (transform/multiply transform adj)
                                              scale scale)
                                     (transform/invert adj)))))

(defn- translate [sm x y]
  (let [{:keys [weight transform]} sm
        inv (/ 1 weight)]
    (assoc sm :transform (transform/translate transform (* inv x) (* inv y)))))

(defn draw! [starmap scene]
  (let [{:keys [canvas]} scene
        {:keys [tile-size tile-stars seed transform]} starmap
        width (canvas/width canvas)
        height (canvas/height canvas)
        [min-x min-y] (map #(tile-index % tile-size)
                           (transform/transform-point transform 0 0))
        [max-x max-y] (map #(tile-index % tile-size)
                           (transform/transform-point transform width height))
        tiles (for [x (range min-x (inc max-x))
                    y (range min-y (inc max-y))]
                [x y])
        context (canvas/context canvas)
        inv-transform (transform/invert transform)]
    (canvas/save! context)
    (canvas/set-transform! context (transform/create))
    (canvas/fill-style! context "#ffffff")
    (doseq [[x y] tiles]
      (draw-tile! inv-transform context seed x y tile-size tile-stars))
    (canvas/restore! context)))
