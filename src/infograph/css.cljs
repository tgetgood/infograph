(ns infograph.css
  (:require [re-frame.core :as re-frame]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; Helpers
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn vapply
  "Concats last argument into vector of all but last arguments. Basically apply
  for vectors but without execution.

  This is important for building higher level components where we don't want
  dynamic lists in react (things that will not change). An alternative would be
  to use random uuids as keys, but I don't see any functional difference between
  the two approaches."
  [& args]
  (into (vec (butlast args)) (last args)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; Grid
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def numbers
  "Converts numerals into gramatically correct English.
  Great example of why trying to code in English generally makes life more
  difficult."
  (into ["columns zero" "column one"] ;<-- Notice the singular "column"
        (map #(str "columns " %)
          ["two" "three" "four" "five" "six" "seven" "eight"
           "nine" "ten" "eleven" "twelve"])))

(defn calculate-width [n c]
  ;;TODO: Make sure that the numbers add up to something logical.
  ;;TODO: What do we do if the quotient below is not an integer? 
  (get numbers
       (if-let [width (:width (meta c))]
         width
         (if (= n 1)
           1
           (quot 12 n)))))

(defn columnize [opts n c]
  [:div (merge opts {:class (calculate-width n c)}) c])

(defn row [opts & args]
  (if (map? opts)
    (vapply :div.row opts (map (partial columnize opts (count args)) args))
    (apply row {} opts args)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; Widgets
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn button [& content]
  (vapply :a.button content))

(defn thumbnail [shape]
  (let [thumbnail-url (re-frame/subscribe [:shape-thumbnail shape])]
    (fn []
      (println @thumbnail-url)
      [:img {:src @thumbnail-url
             :id shape
             :draggable false
             :style {:height "38px"
                     :width "38px"}}])))
