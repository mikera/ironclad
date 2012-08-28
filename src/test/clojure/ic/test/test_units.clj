(ns ic.test.test-units
  (:use clojure.test)
  (:use [ic protocols map units])
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
    (validate-unit u)))

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

