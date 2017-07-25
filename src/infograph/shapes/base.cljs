(ns infograph.shapes.base
  (:require [infograph.canvas :as canvas]
            [infograph.protocols :as protocols]
            [infograph.shapes.impl :as impl]
            [infograph.window :as window]))

;; FIXME: I've really defined 3 distinct Line types here. Dynamic, Cartesian,
;; and Pixelated. These should probably be three different types. Honestly the
;; only reason I'm using records is for polymorphism is the Drawable,
;; Instantiable, and Projectable interfaces. That's no excuse. Having separate
;; types would make errors in the data pipeline order more apparent and more
;; easily diagnosable.
;;
;; REVIEW: The identical nature of the various protocol implementations leads me
;; to believe that there's a better level of abstraction at which to view
;; shapes. Instantiation and projection are necessarily identical for anything
;; made of lines. The radius of a circle seems different though; it's a scalar
;; so it needs to be adjusted without accounting for the origin. The api calls
;; will still be the same if I get the polymorphism right.
;;
;; Basically there's a deeper structure ready to be uncovered.

(extend-protocol protocols/Projectable
  cljs.core/PersistentVector
  (project [this w]
    (assert (= 2 (count this)) "Project only implemented in 2D.")
    (window/project w this))

  number
  (project [this w]
    (window/project-scalar w this))

  nil
  (project [_ _]
    #_(.error js/console "Projecting nil")))

(defrecord Line [style p q]
  protocols/Drawable
  (protocols/draw! [_ ctx]
    (when q
      (canvas/line ctx style p q)))

  protocols/Projectable
  (protocols/project [_ w]
    (Line. style (protocols/project p w) (protocols/project q w)))

  impl/Instantiable
  (impl/instantiate [_ data]
    (Line. style (impl/instantiate p data) (impl/instantiate q data))))

(defrecord Rectangle [style p q]
  protocols/Drawable
  (protocols/draw! [_ ctx]
    (canvas/rectangle ctx style p q))

  impl/Instantiable
  (impl/instantiate [_ data]
    (Rectangle. style (impl/instantiate p data) (impl/instantiate q data))))

(defrecord Circle [style c r]
  protocols/Drawable
  (protocols/draw! [this ctx]
    (canvas/circle ctx style c r))

  protocols/Projectable
  (protocols/project [_ w]
    (Circle. style (protocols/project c w) (protocols/project r w)))

  impl/Instantiable
  (impl/instantiate [_ data]
    (Circle. style (impl/instantiate c data) (impl/instantiate r data))))
