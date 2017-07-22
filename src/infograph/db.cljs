(ns infograph.db
  (:require [cljs.spec.alpha :as spec]
            [infograph.input :as input]
            [infograph.shapes :as shapes]
            [re-frame.db :as re-frame.db]))

;;;; Geometry
;; TODO: Gather these somewhere else.

(spec/def ::coord
  (spec/and vector? #(= 2 (count %)) #(every? number? %)))

(spec/def ::maybe-coord
  (spec/or :nil nil? :coord ::coord))

;;;;; DB

(spec/def ::db
  (spec/keys :req-un [::data ::input ::canvas]))

;;;;; Input

(spec/def ::input
  (spec/keys :req-un [::strokes ::drag-position]))

(spec/def ::strokes
  (spec/and vector? (spec/* ::stroke)))

(spec/def ::stroke
  (spec/keys :req-un [::start ::current ::end]))

(spec/def ::start ::maybe-coord)
(spec/def ::current ::maybe-coord)
(spec/def ::end ::maybe-coord)

;;;;; Canvas

(spec/def ::canvas
  (spec/keys :req-un [::shape ::input-mode ::window]))

(spec/def ::window
  (spec/keys :req-un [::zoom ::origin ::width ::height]))

(spec/def ::width int?) ;FIXME: Will fail if screens get really big.
(spec/def ::height int?)
(spec/def ::zoom (spec/and number? #(< 0 %)))
(spec/def ::origin ::coord)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; DB Template
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def default-db
  {:data input/nice-map
   :input {:strokes [{:start nil :current nil :end nil}]
           :drag-position [0 0]}
   :canvas {:shape (shapes/empty-composite)
            :input-mode :grab
            :window {:zoom 1
                     :origin [1 0]
                     :width 0
                     :height 0}}})


(defn db-valid? []
  (spec/valid? ::db @re-frame.db/app-db))
