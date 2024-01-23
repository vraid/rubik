(ns rubik.graphics
  (:require [thi.ng.geom.gl.core :as gl]
            [thi.ng.geom.gl.shaders :as sh]
            [thi.ng.geom.gl.shaders.basic :as sh-basic]
            [thi.ng.geom.gl.webgl.constants :as glc]
            [thi.ng.geom.matrix :as mat]))

(def default-shader (sh-basic/make-shader-spec-2d true))

(defn make-shader [gl]
  (sh/make-shader-from-spec gl default-shader))

(defn buffer-insert [buffer size data offset]
  (when (seq data)
    (.set buffer (first data) (* size offset))
    (recur buffer size (rest data) (inc offset))))

(defn vertex-data [vertex-buffer color-buffer face-count]
  {:attribs
   {:position
    {:size 3
     :data vertex-buffer}
    :color
    {:size 4
     :data color-buffer}}
   :mode 4
   :num-vertices (* 3 face-count)
   :num-faces face-count})

(defn to-vertices [vertex-buffer color-buffer n color center edges]
  (loop [n n a (first edges) ls (rest edges)]
    (buffer-insert color-buffer 4 (repeat 3 color) (* 3 n))
    (if (seq ls)
      (let
       [b (first ls)]
        (buffer-insert vertex-buffer 3 [center a b] (* 3 n))
        (recur (inc n) b (rest ls)))
      (inc n))))

(defn triangles [to-vertices n {:keys [pieces color centered? center center-edges]}]
  (if centered?
    (let
     [edges (mapv :edges pieces)
      black #js [0 0 0 1]
      center #js [0 0 0]
      square [#js [-100 -100 0] #js [100 -100 0] #js [100 100 0] #js [-100 100 0] #js [-100 -100 0]]
      n (to-vertices n color center square)]
      (to-vertices n black center (reverse (concat (first edges) (mapcat rest (rest edges))))))
    (let
     [n (to-vertices n color center center-edges)]
      (loop [n n
             pieces pieces]
        (if (seq pieces)
          (let
           [piece (first pieces)]
            (recur (to-vertices n color (:center piece) (:edges piece)) (rest pieces)))
          n)))))

(defn fill-buffers [to-vertices squares]
  (loop [n 0 squares squares]
    (if (seq squares)
      (recur (triangles to-vertices n (first squares)) (rest squares))
      n)))

(defn draw-canvas [canvas shader scale buffers data]
  (let [gl (gl/gl-context canvas)
        view-rect (gl/get-viewport-rect gl)
        h scale
        w h
        {vertex-buffer :vertices
         color-buffer :colors} buffers
        _ (.fill vertex-buffer 0)
        _ (.fill color-buffer 0)
        face-count (fill-buffers (partial to-vertices vertex-buffer color-buffer)
                                 data)
        model (-> (vertex-data vertex-buffer color-buffer face-count)
                  (gl/make-buffers-in-spec gl glc/static-draw)
                  (update :uniforms merge {:proj (gl/ortho (- w) h w (- h) -1 1)
                                           :model mat/M44})
                  (assoc :shader shader))]
    (gl/set-viewport gl view-rect)
    (gl/cull-faces gl glc/back)
    (gl/clear-color-and-depth-buffer gl 0 0 0 1 1)
    (gl/draw-with-shader gl model)))
