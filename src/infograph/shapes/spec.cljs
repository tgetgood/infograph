(ns infograph.shapes.spec
    (:require [cljs.spec.alpha :as spec]))

;; FIXME: This is a very cursory set of properties. Just enough for a useful
;; proof of concept. A full redesign of the specs might be necessary when I go
;; through all the properties in canvas, or something like Illustrator, but so
;; be it, this is an experiment after all.
(spec/def ::stroke
  (spec/keys :req [::colour ::width]))

(spec/def ::fill
  (spec/keys :req [::colour]))

(spec/def ::style
  (spec/keys :req [::stroke ::fill]))

;; NOTE: All shapes are defined in multiple ways. That can be by coordinates of
;; control points, by dimensions, by an affine transformation of a normalised
;; exemplar, etc..
;;
;; This ambiguity is intentional so that you can choose the representation of a
;; shape most conducive to manipulation by the data you have.
;;
;; Hopefully I can make this clearer by demonstration.
;;
;; NOTE: I want to define a minimal toolkit of shapes and a simple way to
;; register new ones. For example lines, circles, and general polygons will be
;; built in, but regular hexagons should come from a library. Compound shapes
;; that are ubiquitous like squares should be included in the core just because
;; people will be looking for them.
;;
;; Remember: compact, simple core language with powerful extensability. That's
;; the way to go.

(spec/def ::affine-tx
  ;; Not a useful representation except for mathematicians...
  ;;
  ;; So how do we represent affine transformations? There are 4 components:
  ;; scale, rotation, reflection, and translation. The problem is that they
  ;; don't commute, so we need either an imperative style language, or a
  ;; cannonical order.
  ;;
  ;; A cannonical order is a recipe for bad assumptions. It will make some
  ;; things easy and others impossible. If the relation between the data and the
  ;; shape is expressible in a couple of sentences or hand motions then it
  ;; should be simple to implement. All affine transformations are so
  ;; explicable.
  ;;
  ;; So we're left with a kind of affine tx DSL that needs to be figured
  ;; out. Yay. Well at least whatever we come up with will compile down to y =
  ;; Mx + b, so at the bottom level this spec is technically correct...
  (spec/keys :req [::M ::b]))

(spec/def ::line
  (spec/keys :req [(or ::affine-tx [::start ::end]) ::style]))

(spec/def ::circle
  (spec/keys :req [::radius ::centre ::style]))

(spec/def ::ellipse
    (spec/keys :req [::a ::b ::centre ::style]))

(spec/def ::rectangle
    (spec/keys :req []))

(spec/def ::square
    (spec/keys :req []))

(spec/def ::polygon
    (spec/keys :req [::points]))

(spec/def ::points
  (spec/* ::point))

(spec/def ::point
  (spec/and vector? #(= 2 (count %)) #(every? number? %)))
