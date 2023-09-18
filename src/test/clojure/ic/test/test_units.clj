(ns ic.test.test-units
  (:require [clojure.test :refer [deftest is]]
            [ic.command :refer [command]]
            [ic.engine :refer [add-unit get-unit location-of-unit new-game
                               player point set-terrain terrain]]
            [ic.game :refer [apply-command]]
            [ic.protocols :refer [add-player validate]]
            [ic.units :as units :refer :all])
  (:require [ic protocols map engine command game units])
  (:require [mc.util :as mcu]))


(deftest test-identity
  (let [u (units/unit "Steam Tank")]
    (is (= (ic.units/unit "Steam Tank") (ic.units/unit-type-map "Steam Tank")))
    (is (= u (units/unit "Steam Tank")))))


(deftest t2
  (let [u (units/unit "Steam Tank")]
    (is (= "Steam Tank" (:name u)))
    (is (= 100 ((TARGET_EFFECT ATTACKTYPE_DEATH_RAY) UNITTYPE_INFANTRY)))
    (is (not (nil? type)))
    (is (not (nil? (:abilities u))))
    (is (not (nil? (find-ability u "6\" Steam Cannon"))))
    (validate u)))

(defn test-abilities [u a]
  (if (:is-build a)
    (doseq [bt (:build-list a)]
      (is (units/unit-type-map bt))))
  (if (:is-attack a)
    (doseq [tu units/unit-types]
      (is (>= (units/attack-power a u tu) 0)))))

(deftest test-unit-types
  (doseq [u units/unit-types]
    (is (not (nil? (:contents u))))
    (doseq [a (:abilities u)]
      (test-abilities u a))
    (doseq [t ic.map/terrain-types]
      (is (units/base-move-cost u t)))
    (doseq [u2 units/unit-types]
      (units/can-enter? u u2))))


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
    (is (can-move? g u 1 0 0 0)) ;; onto occupied hex 
    (is (not (can-move? g u 0 0 0 1)))))

(deftest test-unit-move-command
  (let [g (unit-test-game)
        u (unit "Rifles")
        g (-> g
            (add-unit 0 0 u ))
        u (get-unit g 0 0)      ;; get unit back, should now have a valid ID
        cm (command u "Move: Foot" 1 0 nil)
        g2 (apply-command g cm)]
    (is (= (point 1 0) (location-of-unit g2 u))
        (< (:aps u) (:aps (get-unit g2 1 0))))))
