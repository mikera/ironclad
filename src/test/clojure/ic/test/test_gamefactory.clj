(ns ic.test.test-gamefactory
  (:use clojure.test)
  (:use [ic protocols engine units game gamefactory]))

(deftest t-factory
  (let [g (make-game)]
    (validate g)))


(defn test-game []
  (let [g (new-game)] 
	  (-> g 
	    (assoc :terrain test-map)
	    (add-player 
	      (player
	        {:side 0 
	         :is-human true 
	         :ai-controlled false})))))


(deftest t2
  (let [tg (test-game)
        u (unit "Steam Tank" {:player-id (:id (get-player-for-side tg 0))})
        g (-> tg
            (add-unit 2 2 u))]
    (is (not (nil? (get-unit g 2 2))))
    (is (not (nil? (first (:players g)))))
    (is (> (:aps u) 0 ))
    (is (== 1.0 (move-cost g u 2 2 2 3)))
    (validate g)))