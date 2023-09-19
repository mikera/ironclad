(ns ic.test.test-serial
  (:require [clojure.test :refer [deftest is]]
            [ic.units :as units])
  (:require [mc.util :as mcu])
  (:require 
   [ic.serial :refer [decode encode deserialize serialize]]
   [ic.map]
   [ic.engine :as eng :refer [player]]
   [ic protocols map units serial]))

(defn coded-ok? [x]
  (and
    (= x (decode (encode x)))
    (= x (deserialize (serialize x)))))

(deftest test-primitives []
  (is (coded-ok? (Integer. 1)))
  (is (coded-ok? (Long. 1)))
  (is (coded-ok? :foo))
  (is (coded-ok? "bar"))
  (is (coded-ok? true))) 

(deftest test-structures []
  (let [m [{:a {} 
            :b ["Hello" "World"]
            :c nil
            :cc 2
            :d (eng/new-map)
            :e (mikera.persistent.LongMap/EMPTY)}]] 
    (is (= m (decode (encode m))))
    (is (= m (deserialize (serialize m))))))


(deftest test-map []
  (let [m {:a 1 :b "Hello" :foo :bar}] 
    (is (= {} (mcu/map-difference (deserialize (serialize m)) m)))
    (is (= m (deserialize (serialize m))))))

(deftest test-longmap []
  (let [m1 (mikera.persistent.LongMap/EMPTY)
        m2 (.include m1 2 "Hello")
        m3 (.include m2 3 (mikera.persistent.LongMap/EMPTY))] 
    (is (= m1 (decode (encode m1))))
    (is (= m2 (decode (encode m2))))
    (is (= m3 (decode (encode m3))))
    (is (= m1 (deserialize (serialize m1))))
    (is (= m2 (deserialize (serialize m2))))
    (is (= m3 (deserialize (serialize m3))))))


(deftest test-basic-types []
  (let [a (eng/point 1 2)
        b (units/unit "Steam Tank")
        c (ic.map/terrain "Grassland")
        d (player {:foo :bar})] 
    (is (= nil (deserialize (serialize nil))))
    (is (= a (deserialize (serialize a))))
    (is (= b (decode (encode b))))
    (is (= d (decode (encode d))))
    (is (= {} (mcu/map-difference d (deserialize (serialize d)))))
    (is (= d (deserialize (serialize d))))
    (is (= {} (mcu/map-difference b (deserialize (serialize b)))))
    (is (= b (deserialize (serialize b))))
    (is (= c (deserialize (serialize c))))))


(deftest test-game []
  (let [g (ic.gamefactory/make-game)
        pl (:players g) 
        cg (assoc g :unit-locations nil)
        e1 (first pl)
        e2 (first (deserialize (serialize pl)))] 
    (is (= (.getValue e1) (.getValue e2)))
    (is (.equals (.getValue e1) (.getValue e2)))
    (is (= (first pl) (first (deserialize (serialize pl)))))
    (is (= pl (deserialize (serialize pl))))
    (is (= {} (mcu/map-difference g (deserialize (serialize g)))))
    (is (= g (deserialize (serialize g))))))
