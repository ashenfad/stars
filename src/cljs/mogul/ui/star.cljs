(ns mogul.ui.star
  (:require [mogul.ui.canvas :as canvas]))

(def two-pi (* 2 Math/PI))
(def base-size 10)
(def phase-factor 0.35)

(defn create [id location]
  {:id id
   :location location})

(defn- draw-core! [context color size location]
  (let [radius (+ base-size (* base-size size))
        gradient (canvas/radial-gradient! context location 0 location radius)
        gradient (if (> size 0.85)
                   (-> gradient
                       (canvas/color-stop! 0.2 "white")
                       (canvas/color-stop! 0.25 (canvas/paint color 0.78))
                       (canvas/color-stop! 0.5 (canvas/paint color 0.12))
                       (canvas/color-stop! 0.75 (canvas/paint color 0.24))
                       (canvas/color-stop! 1.0 (canvas/paint color 0.0)))
                   (-> gradient
                       (canvas/color-stop! 0.15 "white")
                       (canvas/color-stop! 0.2 (canvas/paint color 0.78))
                       (canvas/color-stop! 0.6 (canvas/paint color 0.12))
                       (canvas/color-stop! 1.0 (canvas/paint color 0.0))))]
    (-> context
        (canvas/fill-style! gradient)
        (canvas/begin-path!)
        (canvas/circle! location radius)
        (canvas/close-path!)
        (canvas/fill!))))

(defn- make-path! [context points]
  (canvas/begin-path! context)
  (apply canvas/move-to! context (first points))
  (doseq [[x y] (next points)]
    (canvas/line-to! context x y))
  (canvas/close-path! context))


(defn- draw-spokes! [context color size location phase]
  (let [phase-fn (fn [offset]
                   (+ (* phase-factor (/ (inc (Math/sin (+ offset phase))) 2))
                      (- 1 phase-factor)))
        [p1 p2] (map phase-fn [2.1 4.2])
        radius (* 3 (+ base-size (* base-size size)))
        spoke (* p1 radius)
        inner (/ spoke 20)
        [loc-x loc-y] location
        xs (map (partial + loc-x)
                [inner spoke inner 0 (- inner) (- spoke) (- inner) 0])
        ys (map (partial + loc-y)
                [inner 0 (- inner) (- spoke) (- inner) 0 inner spoke])]
    (-> context
        (canvas/fill-style! (canvas/paint color 0.1))
        (make-path! (map list xs ys))
        (canvas/fill!))))

(defn draw! [context star]
  (let [{:keys [location seed]} star
        [x y] location
        rnd (js/Alea. seed)
        size (rnd)
        phase (* (rnd) two-pi)
        color (repeatedly 3 rnd)
        color (map (fn [c]
                     (min 255 (Math/floor (+ 160 (* 200 (/ c  (reduce + color)))))))
                   color)]
    (-> context
        (draw-spokes! color size location phase)
        (draw-core! color size location))))
