(ns infograph.protocols)

(defprotocol Drawable
  "High level interface to the canvas window. Main app should only ever interact
  with it at this level."
  (draw! [this ctx] "Draw this in the given window"))

(extend-protocol Drawable
  nil
  (draw! [_ _]
    (.error js/console "draw! called on nil")))


;;TODO: So where does this fit in now?
#_(draw? [this ctx] "Is this visible in the given window?")


(defprotocol Projectable
  (project [this window] "Project this shape through the given window."))

;;FIXME: What a name.
(defprotocol CoProjectable
  (coproject [this window] "inject this event into the window"))

