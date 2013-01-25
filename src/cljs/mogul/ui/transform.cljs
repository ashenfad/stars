(ns mogul.ui.transform)

(defn create
  "The identity transform"
  []
  [1 0 0 1 0 0])

(defn multiply
  "Returns the multiplied transform"
  [matrix1 matrix2]
  (let [[a11 a12 a21 a22 ax ay] matrix1
        [b11 b12 b21 b22 bx by] matrix2]
    [(+ (* a11 b11) (* a21 b12))
     (+ (* a12 b11) (* a22 b12))
     (+ (* a11 b21) (* a21 b22))
     (+ (* a12 b21) (* a22 b22))
     (+ (* a11 bx) (* a21 by) ax)
     (+ (* a12 bx) (* a22 by) ay)]))

(defn invert
  "Returns an inverted transform"
  [matrix]
  (let [[m11 m12 m21 m22 dx dy] matrix
        d (/ 1 (- (* m11 m22) (* m12 m21)))]
    [(* d m22)
     (- (* d m12))
     (- (* d m21))
     (* d m11)
     (* d (- (* m21 dy) (* m22 dx)))
     (* d (- (* m12 dx) (* m11 dy)))]))

(defn rotate
  "Returns a rotated transform"
  [matrix rad]
  (let [[m11 m12 m21 m22 dx dy] matrix
        c (Math/cos rad)
        s (Math/sin rad)]
    [(+ (* c m11) (* s m21))
     (+ (* c m12) (* s m22))
     (- (* c m21) (* s m11))
     (- (* c m22) (* s m12))
     dx
     dy]))

(defn translate
  "Returns a translated transform"
  [matrix x y]
  (let [[m11 m12 m21 m22 dx dy] matrix]
    [m11
     m12
     m21
     m22
     (+ dx (* x m11) (* y m21))
     (+ dy (* x m12) (* y m22))]))

(defn scale
  "Returns a scaled matrix"
  [matrix sx sy]
  (let [[m11 m12 m21 m22 dx dy] matrix]
    [(* sx m11)
     (* sx m12)
     (* sy m21)
     (* sy m22)
     dx
     dy]))

(defn transform-point
  "Transforms a point"
  ([matrix x y]
     (let [[m11 m12 m21 m22 dx dy] matrix]
       [(+ (* x m11) (* y m21) dx)
        (+ (* x m12) (* y m22) dy)]))
  ([matrix pt]
    (apply transform-point pt)))
