(ns infograph.db
  (:require [cljs.spec.alpha :as spec]
            [infograph.input :as input]
            [infograph.shapes :as shapes]
            [re-frame.db :as re-frame.db]))

;;;;; Dummy dataset

(def election-data
  {:registered    25939742
   :voted         17711983
   :valid-ballots 17591468
   :seats         {:liberal      184
                   :conservative 99
                   :ndp          44
                   :bloc         10
                   :green        1
                   :other        0}
   :votes         {:liberal      6930136
                   :conservative 5600469
                   :ndp          3461262
                   :bloc         818652
                   :green        695864
                   :other        85085}})
(def party-names
  {:liberal      "Liberal"
   :conservative "Conservative"
   :ndp          "NDP"
   :bloc         "Bloc Québécois"
   :green        "Green Party"
   :other        "Other"})

(def colours
  "Colours stolen cut, paste, and sinker from wikipedia markup."
  {:liberal      "#EA6D6A"
   :conservative "#6495ED"
   :ndp          "#F4A460"
   :bloc         "#87CEFA"
   :green        "#99C955"
   :other        "grey"})

;;;; Geometry
;; TODO: Gather these somewhere else.

(spec/def ::coord
  (spec/and vector?
            #(= 2 (count %))
            #(every? number? %)
            #(not (some js/isNaN %))))

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
  (spec/keys :req-un [::zoom ::origin ::width ::height ::offset]))

(spec/def ::width int?) ;FIXME: Will fail if screens get really big.
(spec/def ::height int?)
(spec/def ::zoom (spec/and number? #(< 0 %)))
(spec/def ::origin ::coord)
(spec/def ::offset (spec/and vector? #(= 2 (count %)) #(every? int? %)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; DB Template
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def default-db
  {:data   {:data  election-data
            :focus {:open?    true
                    :children {}}}
   :input  {:strokes       [{:start nil :current nil :end nil}]
            :drag-position nil}
   :property-window nil
   :canvas {:shape      (shapes/empty-frame)
            :input-mode :grab
            :window     {:zoom   1
                         :origin [0 0]
                         :offset [0 0]
                         :width  0
                         :height 0}}})


(defn spec-db []
  (spec/explain ::db @re-frame.db/app-db))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; Query fns
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn inst-data [db]
  {:data  (-> db :data :data)
   :input (-> db :input)})

(def window-path [:canvas :window])

(defn window [db]
  (get-in db window-path))
