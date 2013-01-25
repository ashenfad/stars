(ns mogul.ui.scene
  (:require [clojure.browser.dom :as dom]
            [goog.events :as events]
            [goog.events.MouseWheelHandler :as MouseWheelHandler]
            [mogul.ui.star :as star]
            [mogul.ui.canvas :as canvas]
            [mogul.ui.transform :as transform]
            [mogul.ui.background :as background]
            [mogul.game-gen :as game-gen]))

(def scene (atom {}))

(def zoom-in 1.08)
(def zoom-out (/ 1 zoom-in))

(def max-zoom-steps 12)
(def max-zoom (Math/pow zoom-in max-zoom-steps))
(def min-zoom (Math/pow zoom-out max-zoom-steps))

(defn add-background-layer! [starmap]
  (swap! scene assoc :backgrounds (conj (:backgrounds @scene)
                                              starmap)))

(defn- draw-base! []
  (let [{:keys [canvas background-color]} @scene
        width (canvas/width canvas)
        height (canvas/height canvas)
        context (canvas/context canvas)]
    (canvas/save! context)
    (canvas/set-transform! context 1 0 0 1 0 0)
    (canvas/fill-style! context background-color)
    (canvas/fill-rect! context 0 0 width height)
    (canvas/restore! context)))

(defn- draw-background! []
  (let [{:keys [canvas backgrounds]} @scene]
    (doseq [layer backgrounds]
      (background/draw! layer @scene))))

(defn draw-game! [context game]
  (let [star-map (reduce (fn [m v] (assoc m (:id v) v)) {} (:stars game))
        white [255 255 255]]
    (doseq [star (vals star-map)]
      (star/draw! context star))
    (canvas/save! context)
    (canvas/line-width! context 3)
    (doseq [edge (:edges game)]
      (let [[s1 s2] (:stars edge)
            [x1 y1] (:location (star-map s1))
            [x2 y2] (:location (star-map s2))
            alpha (max 0.08 (- 0.4 (* (:until-bid edge) 0.08)))]
        (-> (canvas/begin-path! context)
            (canvas/stroke-style! (canvas/paint white alpha))
            (canvas/move-to! x1 y1)
            (canvas/line-to! x2 y2)
            (canvas/close-path!)
            (canvas/stroke!))))
    (canvas/restore! context)))

(defn draw! []
  (let [{:keys [transform canvas game]} @scene
        context (canvas/context canvas)]
    (canvas/set-transform! context (transform/create))
    (draw-base!)
    (draw-background!)
    (canvas/set-transform! context (transform/invert transform))
    (draw-game! context game)))

(defn window-resize! []
  (let [canvas (:canvas @scene)]
    (set! (.-width canvas) (. js/window -innerWidth))
    (set! (.-height canvas) (. js/window -innerHeight)))
  (draw!))

(defn handle-mousedown! [event]
  (let [x (. event -clientX)
        y (. event -clientY)]
    (swap! scene assoc :last-mouse [x y])))

(defn handle-mouseup! [_]
  (swap! scene dissoc :last-mouse))

(defn- translate! [x y]
  (let [{:keys [transform backgrounds]} @scene]
    (swap! scene assoc
           :transform  (transform/translate transform x y)
           :backgrounds (map #(background/translate % x y) backgrounds))))

(defn- scale-on! [scale x y]
  (let [{:keys [transform zoom backgrounds]} @scene
        t (transform/translate (transform/create) x y)
        zoom (* scale zoom)]
    (when (<= min-zoom zoom max-zoom)
      (swap! scene assoc
             :zoom zoom
             :transform (transform/multiply (transform/scale (transform/multiply transform t) scale scale)
                                    (transform/invert t))
             :backgrounds (map #(background/scale % scale x y) backgrounds)))))

(defn handle-mousemove! [event]
  (when-let [last-mouse (:last-mouse @scene)]
    (let [[old-x old-y] last-mouse
          new-x (. event -offsetX)
          new-y (. event -offsetY)]
      (swap! scene assoc :last-mouse [new-x new-y])
      (translate! (- old-x new-x) (- old-y new-y))
      (draw!))))

(defn handle-wheel! [event]
  (scale-on! (if (pos? (. event -deltaY)) zoom-out zoom-in)
             (. event -offsetX)
             (. event -offsetY))
  (draw!))

(defn- center-on! [x y]
  (let [{:keys [transform canvas zoom backgrounds]} @scene
        mid-x (- x (* 0.5 zoom (canvas/width canvas)))
        mid-y (- y (* 0.5 zoom (canvas/height canvas)))
        [tx ty] (transform/transform-point (transform/invert transform) mid-x mid-y)]
    (translate! tx ty)))

(defn center-on-star! [star-id]
  (apply center-on! (:location
                     (first (filter #(= star-id (:id %))
                                    (:stars (:game @scene))))))
  (draw!))

(defn center-on-edge! [edge-id]
  (let [{:keys [stars edges]} (:game @scene)
        star-map (reduce (fn [m v] (assoc m (:id v) v)) {} stars)
        edge (first (filter #(= edge-id (:id %)) edges))
        [s1 s2] (map star-map (:stars edge))
        sums (map + (:location s1) (:location s2))]
    (center-on! (/ (first sums) 2)
                (/ (second sums) 2))
    (draw!)))

(defn enable-mouse! []
  (let [canvas (:canvas @scene)]
    (events/listen canvas "mousedown" handle-mousedown!)
    (events/listen canvas "mouseup" handle-mouseup!)
    (events/listen canvas "mousemove" handle-mousemove!)
    (events/listen (events/MouseWheelHandler. canvas) "mousewheel" handle-wheel!)))

(defn enable-resize! []
  (events/listen js/window "resize" window-resize!))

(defn create! [canvas-id]
  (swap! scene assoc
         :game (game-gen/create :stars 20)
         :transform (transform/create)
         :zoom 1
         :canvas (dom/get-element canvas-id)
         :backgrounds [(background/create 1024 16 1.1)
                       ;; (background/create 1024 16 1.3)
                       (background/create 1024 16 1.5)
                       ;; (background/create 1024 16 1.7)
                       (background/create 1024 16 1.9)]
         :background-color "#000000")
  (enable-resize!)
  (enable-mouse!)
  (window-resize!)
  (center-on-star! 0))
