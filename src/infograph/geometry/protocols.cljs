(ns infograph.geometry.protocols
  (:refer-clojure :exclude [+ - *]))

;; REVIEW: Can I commandeer the number 1 so that it represents the unit of any
;; monoid? Should I? Make a Unit type that does the right thing polymorphically.
(defprotocol Geometric
  (+ [this x])
  (- [this] [this x])
  (* [this] [this x]))

(defprotocol IVector
  (dot [this v])
  (norm [this]))
