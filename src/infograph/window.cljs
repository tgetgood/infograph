(ns infograph.window
  "Functions to map the cartesian plane into a window of pixels and vice
  versa.")

(defn- zoom-factor
  [dz]
  (let [base 2
        stretch 100]
    (js/Math.pow base (/ dz stretch))))

(defn- adjust-origin
  "Given an origin, a centre of zoom and a zoom scale, return the new
  origin."
  [[x y] [zx zy] dz]
  (let [delta (zoom-factor dz)]
    [(* (- zx x) delta) (* (- zy y) delta)]))

(defn- adjust-zoom
  "Reducing function for zoom events. Currently just an exponential."
  [z dz]
  (* z (zoom-factor dz)))

(defn zoom-window
  "Returns a new window map accounting for zoom factor z centred at location
  zc."
  [w dz zc]
  (-> w
      (update :origin adjust-origin zc dz)
      (update :zoom adjust-zoom dz)))

(defn pan-window
  "Returns a new window panned by vector v"
  [w v]
  (update w :origin (partial mapv + v)))

(defn on-screen?
  "Returns true if the point [x y] is in the given window. The window is assumed
  to include its boundaries."
  [[x y] {[ox oy] :origin h :height w :width z :zoom}]
  (and (<= ox x (+ ox (* z w)))
       (<= oy y (+ oy (* z h)))))

(defn invert
  "Converts normal plane coordinates (origin in the bottom left) to standard
  computer graphics coordinates (origin in the top left)"
  [{h :height} [x y]]
  [x (- h y)])

(defn project
  "Project coordinates from the cartesian plane onto pixel coordinates through
  window w."
  [{[ox oy] :origin z :zoom :as w} [x y]]
  (invert w [(* (- x ox) z) (* (- y oy) z)]))

(defn coproject
  "Inverse of projection. Converts pixel coordinates into corresponding
  cartesian coordinates through the given window."
  [{z :zoom [ox oy] :origin :as w} [x y]]
  (invert w [(+ (/ x z) ox) (+ (/ y z) oy)]))
