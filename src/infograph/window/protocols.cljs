(ns infograph.window.protocols
    (:refer-clojure :exclude [contains?]))

(defprotocol Drawable
  "High level interface to the canvas window. Main app should only ever interact
  with it at this level."
  (draw! [this window] "Draw this in the given window")
  (draw? [this window] "Is this visible in the given window?"))

;; On REVIEW: Hell, I don't think these guys should be a protocol. We don't need
;; an object to decide any of this. If the plane is the top level visual object,
;; then it needs to be drawable.
;; REVIEW: ah, ah, ah. I don't think it does. More precisely Drawable is totally
;; the wrong term. A shape needs to know how to interact with a window. What we want is a polymorphic function from Shapes in the plane to Shapes in pixel corrdinates. So the protocol is a projection.

(defprotocol ViewFrame
  "A ViewFrame is the window through which one views the datumplane. The window
  is not responsible for rendering, It only converts from one frame of reference
  to another (and resets itself for a new draw, which sounds like a separate
  concern...)"
  (refresh [this] "Clear and reset the window for a new drawing.")
  (contains? [this coord] "Returns true iff coords are visible in this window.")
  (pixels [this coord]
    "Returns pixel coords in this window for given Cartesian coords.")
  (coords [this px] "Convert pixel coords back to Cartesian coords."))

;; TODO: Separate this into draw entry (or just drawable), coord transform, and
;; event handling?

