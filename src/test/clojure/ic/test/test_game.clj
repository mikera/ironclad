(ns ic.test.test-game
  (:use clojure.test)
  (:use [ic protocols engine game map units]))


(deftest t2
  (let [ng (-> (new-game) 
             (add-player (player {:name "Test player" :side 0 :is-human true}))
             (set-terrain 2 2 (terrain "Grassland")))
        player (get-player-for-side ng 0)
        player-id (:id player)
        u (unit "Steam Tank" {:player-id player-id})
        u2 (unit "Rifles" {:player-id player-id})
        g (-> ng
            (add-unit 2 2 u)
            (add-unit 3 3 u2))]
    (is (not (nil? u)))
    (is (side-has-human? g 0))
    (is (not (side-has-human? g 1)))
    (is (= [0] (active-sides g)))
    (is (not (nil? (get-unit g 2 2))))
    (is (nil? (get-unit (remove-unit g 2 2) 2 2)))
    (validate-game g)
    (ai-evaluation ng player)))

(deftest t-terrain
  (let [g (new-game)
        t (terrain "Grassland")]
    (is (nil? (get-terrain g -5 -5)))
    (is (= t (get-terrain (set-terrain g -5 -5 t) -5 -5)))))





