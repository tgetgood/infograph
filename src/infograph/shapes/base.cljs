(ns infograph.shapes.base
  (:require [infograph.canvas :as canvas]
            [infograph.shapes.impl :as impl]
            [infograph.protocols :as protocols]))

(defrecord Line [style p q]
  protocols/Drawable
  (protocols/draw! [_ ctx]
    (canvas/line ctx style p q))

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

  impl/Instantiable
  (impl/instantiate [_ data]
    (Circle. style (impl/instantiate c data) (impl/instantiate r data))))
