(ns infograph.shapes.base
  (:require [infograph.canvas :as canvas]
            [infograph.shapes.impl :as impl]
            [infograph.window.protocols :as wp]))

(defrecord Line [style p q]
  wp/Drawable
  (wp/draw? [_ window]
    (some (partial wp/contains? window) [p q]))
  (wp/draw! [_ {:keys [ctx] :as window}]
    (canvas/line ctx style (wp/pixels window p) (wp/pixels window p)))

  impl/Instantiable
  (impl/instantiate [_ data]
    (Line. style (impl/instantiate p data) (impl/instantiate q data))))

(defrecord Rectangle [style p q]
  wp/Drawable
  (wp/draw! [_ {:keys [ctx] :as window}]
    (canvas/rectangle ctx style (wp/pixels window p) (wp/pixels window p)))

  impl/Instantiable
  (impl/instantiate [_ data]
    (Rectangle. style (impl/instantiate p data) (impl/instantiate q data))))

(defrecord Circle [style c r]
  wp/Drawable
  (wp/draw! [this {:keys [ctx] :as window}]
    (canvas/circle ctx style (wp/pixels window c) r))

  impl/Instantiable
  (impl/instantiate [_ data]
    (Circle. style (impl/instantiate c data) (impl/instantiate r data))))
