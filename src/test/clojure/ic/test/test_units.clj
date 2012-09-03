(ns ic.test.test-units
  (:use clojure.test)
  (:use [ic protocols map engine units])
  (:import [ic.units.Unit]))


(deftest test-identity
  (let [u (unit "Steam Tank")]
    (is (= (ic.units/unit "Steam Tank") (ic.units/unit-type-map "Steam Tank")))
    (is (= u (unit "Steam Tank")))))


(deftest t2
  (let [u (unit "Steam Tank")]
    (is (= "Steam Tank" (:name u)))
    (is (= 100 ((TARGET_EFFECT ATTACKTYPE_DEATH_RAY) UNITTYPE_INFANTRY)))
    (is (not (nil? type)))
    (is (not (nil? (:abilities u))))
    (is (not (nil? (find-ability u "6\" Steam Cannon"))))
    (validate u)))

(defn test-abilities [u a]
  (if (:is-build a)
    (doseq [bt (:build-list a)]
      (is (unit-type-map bt))))
  (if (:is-attack a)
    (doseq [tu unit-types]
      (is (>= (attack-power a u tu) 0)))))

(deftest test-unit-types
  (doseq [u unit-types]
    (is (not (nil? (:contents u))))
    (doseq [a (:abilities u)]
      (test-abilities u a))
    (doseq [t ic.map/terrain-types]
      (is (base-move-cost u t)))
    (doseq [u2 unit-types]
      (can-enter? u u2))))


(deftest test-effects
  (for [att ATTACK_TYPES ut UNIT_TYPES]
    (is (not (nil? ((TARGET_EFFECT att) ut))))))


(defn unit-test-game []
  (-> (new-game) 
             (add-player (player {:name "Test player" :side 0 :is-human true}))
             (add-player (player {:name "Test AI" :side 1 :is-human false}))
             (set-terrain 0 0 (terrain "Grassland"))
             (set-terrain 1 0 (terrain "Grassland"))
             (set-terrain 0 1 (terrain "Sea"))
             (set-terrain 1 1 (terrain "Woods"))))

(deftest test-moves
  (let [g (unit-test-game)
        u (unit "Rifles")
        g (-> g
            (add-unit 0 0 u ))]
    (is (can-move? g u 0 0 1 0))
    (is (not (can-move? g u 0 0 -1 0))) ;; off map
    (is (not (can-move? g u -1 0 0 0))) ;; onto map
    (is (not (can-move? g u 0 0 0 1)))))
