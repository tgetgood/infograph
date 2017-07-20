(ns infograph.window.protocols
    (:refer-clojure :exclude [contains?]))

(defprotocol Drawable
  "High level interface to the canvas window. Main app should only ever interact
  with it at this level."
  (draw! [this window] "Draw this in the given window")
  (draw? [this window] "Is this visible in the given window?"))

(defprotocol ViewFrame
  (refresh [this] "Clear and reset the window for a new drawing.")
  (contains? [this coord] "Returns true iff coords are visible in this window.")
  (pixels [this coord]
    "Returns pixel coords in this window for given Cartesian coords.")
  (coords [this px] "Convert pixel coords back to Cartesian coords."))

;; HACK:
(extend-protocol Drawable
  nil
  (draw? [_ _] false)
  (draw! [_ _]))
