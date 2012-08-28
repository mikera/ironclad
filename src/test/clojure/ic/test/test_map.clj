(ns ic.test.test-map
  (:use clojure.test)
  (:use [ic protocols engine map])
  (:import [ic.engine Point]))


(set! *warn-on-reflection* true)
(set! *unchecked-math* true)


(deftest points
  (testing "Basic Point"
           
    (let [p (point 1 2)] 
      (is (== 2 (get-y p)))
      (is (== 1 (get-x p))))))

(deftest base-map
  (testing "Basic Map"         
    (let [m (new-map)] 
      (is (= nil (mget m 1 2))))))

(deftest t1
  (let [m (mset (new-map) 2 2 (terrain "Grassland"))
        p (point 2 3)]
    (is (= (point 2 2) (random-point m)))
    (is (= (terrain "Grassland") (mget m 2 2)))
    (is (= 100 (:move-cost-multiplier (terrain "Grassland"))))
    (is (= (.x p) 2))
    (is (= (.y p) 3))))

(deftest tmap
  (let [m (mset (new-map) 2 3 (terrain "Grassland"))]
    (is (= m (mmap m identity)))
    (is (= "Foobar" (mget (mmap m (fn [v] "Foobar")) 2 3)))
    (is (= "Foobar23" (mget (mmap-indexed m (fn [x y v] (str "Foobar" x y))) 2 3)))))

(deftest t11
  (let [p (point 2 3)
        ps (adjacent-point-list p)]
    (is (= 6 (count ps)))
    (is (some #(= (point 3 3) %) ps ))))

(deftest t2
  (let [p (point 2 3)
        q (point 2 3)
        r (point 3 2)]
    (is (instance? Point p))
    (is (= (get-x p) 2))
    (is (= (get-y p) 3))
    (is (= p p))
    (is (= p q))
    (is (= ({p 1} q) 1))
    (is (= ({r 1} q) nil))))