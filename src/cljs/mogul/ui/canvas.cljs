(ns mogul.ui.canvas)

;; width height line-width shadow-blur shadow-color fill-style stroke-style
;; text-align text-baseline font

(defn context [canvas]
  (.getContext canvas "2d"))

(defn canvas [context]
  (. context -canvas))

(defn width [canvas]
  (. canvas -width))

(defn width! [canvas width]
  (set! (.-width canvas) width)
  canvas)

(defn height [canvas]
  (. canvas -height))

(defn height! [canvas height]
  (set! (.-height canvas) height)
  canvas)

(defn line-width [context]
  (. context -lineWidth))

(defn line-width! [context style]
  (set! (.-lineWidth context) style)
  context)

(defn fill-rect! [context x y w h]
  (.fillRect context x y w h)
  context)

(defn clear-rect! [context x y w h]
  (.clearRect context x y w h)
  context)

;; Shadows

(defn shadow-blur [context]
  (. context -shadowBlur))

(defn shadow-blur! [context blur]
  (set! (.-shadowBlur context) blur)
  context)

(defn shadow-color [context]
  (. context -shadowColor))

(defn shadow-color! [context color]
  (set! (.-shadowColor context) color)
  context)

;; Transformations

(defn transform!
  ([context m11 m12 m21 m22 dx dy]
     (.transform context m11 m12 m21 m22 dx dy)
     context)
  ([context matrix]
     (let [[m11 m12 m21 m22 dx dy] matrix]
       (transform! context m11 m12 m21 m22 dx dy)
       context)))

(defn set-transform!
  ([context m11 m12 m21 m22 dx dy]
     (.setTransform context m11 m12 m21 m22 dx dy)
     context)
  ([context matrix]
     (let [[m11 m12 m21 m22 dx dy] matrix]
       (set-transform! context m11 m12 m21 m22 dx dy)
       context)))

(defn translate! [context x y]
  (.translate context x y)
  context)

(defn rotate! [context rad]
  (.rotate context rad)
  context)

(defn scale! [context sx sy]
  (.scale context sx sy)
  context)

;; Context State
(defn save! [context]
  (. context (save))
  context)

(defn restore! [context]
  (. context (restore))
  context)

(defn clear! [context]
  (save! context)
  (transform! context 1 0 0 1 0 0)
  (let [cvs (canvas context)]
    (.clearRect context 0 0 (width cvs) (height cvs)))
  (restore! context)
  context)

;; Paths
(defn move-to! [context x y]
  (.moveTo context x y)
  context)

(defn line-to! [context x y]
  (.lineTo context x y)
  context)

(defn arc
  ([ctx p radius start-angle end-angle]
     (arc ctx p radius start-angle end-angle true))
  ([ctx [x y] radius start-angle end-angle anticlockwise?]
     (.arc ctx x y radius start-angle end-angle anticlockwise?)))

(defn arc-to [ctx [x1 y1] [x2 y2] r]
  (.arcTo ctx x1 y1 x2 y2 r))

(defn quadratic-curve-to [ctx [cx cy] [px py]]
  (.quadraticCurveTo ctx cx cy px py))

(defn bezier-curve-to [ctx [c1x c1y] [c2x c2y] [px py]]
  (.bezierCurveTo ctx c1x c1y c2x c2y px py))

(defn begin-path! [context]
  (. context (beginPath))
  context)

(defn close-path! [context]
  (. context (closePath))
  context)

(defn stroke! [context]
  (. context (stroke))
  context)

(defn fill! [context]
  (. context (fill))
  context)

;;Styles

(defn fill-style [context]
  (. context -fillStyle))

(defn fill-style! [context style]
  (set! (.-fillStyle context) style)
  context)

(defn stroke-style [context]
  (. context -strokeStyle))

(defn stroke-style! [context style]
  (set! (.-strokeStyle context) style)
  context)

;; Text
(defn text-align [context]
  (. context -textAlign))

(defn text-baseline [context]
  (. context -textBaseline))

(defn font [context]
  (. context -font))

(defn stroke-text
  ([context text [x y]]
     (.strokeText context text x y))
  ([context text [x y] max-width]
     (.strokeText context text x y max-width)))

(defn fill-text
  ([context text [x y]]
     (.fillText context text x y))
  ([context text [x y] max-width]
     (.fillText context text x y max-width)))

(defn color
  ([r g b]
     (str "rgba(" r "," g "," b ")"))
  ([r g b a]
     (str "rgba(" r "," g "," b "," a ")")))

(defn paint [[r g b] alpha]
  (color r g b alpha))

;; Gradients
(defn linear-gradient! [context [x1 y1] [x2 y2]]
  (.createLinearGradient context x1 y1 x2 y2))

(defn radial-gradient! [context [x1 y1] r1 [x2 y2] r2]
  (.createRadialGradient context x1 y1 r1 x2 y2 r2))

(defn color-stop! [gradient position color]
  (.addColorStop gradient position color)
  gradient)

(defn circle!
  "A circle with midpoint p and radius r"
  [context p r]
  (arc context p r 0 (* 2 (. js/Math -PI)))
  context)

;; (defn lerp
;;   "Linear interpolation between the points p and q interpolated by
;;   a. a = 0 is at point p, a = 1 is at point q, a = 0.5 is half way
;;   between p and q"
;;   [[px py] [qx qy] a]
;;   [(+ px (* (- qx px) a))
;;    (+ py (* (- qy py) a))])
