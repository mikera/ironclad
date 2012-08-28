(ns ic.test.test-map
  (:use clojure.test)
  (:use [ic protocols map]))

(deftest points
  (testing "Basic Point"
           
    (let [p (point 1 2)] 
      (is (== 2 (get-y p)))
      (is (== 1 (get-x p))))))