(ns infograph.uuid
  (:require [clojure.string :as string]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;; Stolen code from https://github.com/lbradstreet/cljs-uuid-utils
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn squuid
  "(make-random-squuid)  =>  new-uuid
  Arguments and Values:
  new-squuid --- new type 4 (pseudo randomly generated) cljs.core/UUID instance.
  Description:
  Returns pseudo randomly generated, semi-sequential SQUUID. 
  See http://docs.datomic.com/clojure/#datomic.api/squuid
  Returns a UUID where the most significant 32 bits are the current time since epoch in seconds.
  like: xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx as per http://www.ietf.org/rfc/rfc4122.txt.
  Examples:
  (make-random-squuid)  =>  #uuid \"305e764d-b451-47ae-a90d-5db782ac1f2e\"
  (type (make-random-squuid)) => cljs.core/UUID"
  []
  (letfn [(top-32-bits [] (.toString (int (/ (.getTime (js/Date.)) 1000)) 16))
          (f [] (.toString (rand-int 16) 16))
          (g [] (.toString  (bit-or 0x8 (bit-and 0x3 (rand-int 15))) 16))]
    (UUID.(string/join (concat 
                        (top-32-bits) "-"
                        (repeatedly 4 f) "-4"
                        (repeatedly 3 f) "-"
                        (g) (repeatedly 3 f) "-"
                        (repeatedly 12 f))) nil)))
