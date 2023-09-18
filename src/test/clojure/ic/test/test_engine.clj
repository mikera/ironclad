(ns ic.test.test-engine
  (:require 
   [clojure.test :refer [deftest is]])
  (:require
   [ic.engine :refer [player new-game]]
   [ic.protocols :refer [update-player]]))

(defn equal-but-not-identical? [a b]
  (is (= a b))
  (is (not (identical? a b))))

(deftest test-data-equaity []
  (let [p1 (player {:id 1})
        p2 (player {:id 1})
        p3 (assoc p2 :name "Bob")
        ;; note that update-player adds a player if player already has a correct id
        g1 (update-player (new-game) p1)
        g2 (update-player (new-game) p2)] 
    (equal-but-not-identical? p1 p2)
    (is (not= p1 p3))
    (is (= (:units g1) (:units g2)))
    (is (= (:players g1) (:players g2)))
    (is (= g1 g2))))

