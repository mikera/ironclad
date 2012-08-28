(ns ic.serial
  (:use [clojure.test])
  (:use [mc.util])
  (:require [ic.game])
  (:require [ic.gamefactory])
  (:require [ic.map])
  (:import [mikera.util Tools Resource])
  (:use [ic.protocols])
  (:use [clojure.contrib.json]))

(defprotocol PSerial
  "Abstraction for serializable objects to strings"
  (encode [s]))

(declare decode)

(defn serialize-to-json [value]
  (json-str value))

(defn serialize [s]
  (serialize-to-json (encode s)))

(defn deserialize-from-json [string]
  (read-json string false)) ; read withoud keywordizing

(defn encode-map [s]
  (reduce 
    (fn [m [k v]]
      (assoc m (encode k) (encode v)))
    {}
    s))

(extend-protocol PSerial 
  mikera.persistent.SparseMap 
    (encode [s] 
      (let [bounds (.getNonNullBounds s)
            tx (.xmin bounds)
            ty (.ymin bounds)
            w (.getWidth bounds)
            h (.getHeight bounds)]
        ["SparseMap"
            tx
            ty
            w
            h          
           (for [iy (range ty (+ ty h))]
             (for [ix (range tx (+ tx w))]
               (encode (.get s ix iy))))]))

  mikera.persistent.IntMap 
    (encode [s] 
      ["IntMap"
       (reduce
         (fn [m [^Integer i v]]
           (assoc m (str i) (encode v)))
         {}
         s)])
    
    
  ic.units.Unit  
    (encode [s]
      (let [uname (:name s)]
	      ["Unit"
	       uname
	       (encode-map (map-difference s (ic.units/unit-type-map uname)))]))

  ic.map.Terrain  
    (encode [s]
      (let [tname (:name s)]
        ["Terrain"
         tname
         (encode-map (map-difference s (ic.map/terrain-type-map tname)))]))

  ic.player.Player  
    (encode [s]
      ["Player"
       (encode-map s)])

  ic.game.Game  
    (encode [s]
      ["Game"
       (encode-map s)])

    
  ic.map.Point  
    (encode [s]
      ["Point"
         (.x s)
         (.y s)])

    
  clojure.lang.IPersistentMap
    (encode [s]
      (encode-map s))  
    
  clojure.lang.ISeq
    (encode [s]
      (map encode s))  
  
  nil
    (encode [s] 
      nil)  
    
  java.lang.Object
    (encode [s] 
      s))

(defn decode-unit [[dtype uname props]]
  (merge (ic.units/unit-type-map uname) (decode props)))

(defn decode-terrain [[dtype tname props]]
  (merge (ic.map/terrain-type-map tname) (decode props)))

(defn decode-player [[dtype props]]
  (merge (ic.player/player (decode props))))

(defn decode-game [[dtype props]]
  (ic.game.Game. nil (decode props)))

(defn decode-point [[dtype x y]]
  (ic.map/point x y))


(defn decode-sparsemap [[dtype tx ty w h rows]]
  (reduce-indexed 
    (fn [^mikera.persistent.SparseMap sm ^Integer iy row]
      (reduce-indexed
        (fn [^mikera.persistent.SparseMap sm ^Integer ix v]
          (mset sm (+ tx ix) (+ ty iy) (decode v)))
        sm
        row))
    (ic.map/new-map)
    rows))

(defn decode-intmap [[dtype m]]
  (reduce
    (fn [^mikera.persistent.IntMap im [^Integer i v]]
      (.include im (Integer/parseInt i) (decode v)))
    (mikera.persistent.IntMap/EMPTY)
    m))

(defn decode [data]
  (cond
    (map? data)
			(reduce 
        (fn [m [k v]]
          (assoc m (keyword k) (decode v)))
        {}
        data)
    (vector? data)
		  (let [dtype (first data)]
		    (cond
		      (= "Point" dtype)
		        (decode-point data)
		      (= "Terrain" dtype)
		        (decode-terrain data)
		      (= "SparseMap" dtype)
		        (decode-sparsemap data)
          (= "IntMap" dtype)
            (decode-intmap data)
		      (= "Unit" dtype)
		        (decode-unit data)
          (= "Player" dtype)
            (decode-player data)
          (= "Game" dtype)
            (decode-game data)
		      :default
		        (vec (doall (map decode data)))))   
      :default 
        data))


(defn deserialize [string]
  (let [data (deserialize-from-json string)]
    (decode data)))


(defn load-game-from-file [file]
  (ic.serial/deserialize (Tools/readStringFromFile file)))

(defn load-game-from-resourcename [filename]
  (ic.serial/deserialize (Tools/readStringFromStream (Resource/getResourceAsStream filename))))


(deftest test-structures []
  (let [m [{:a {} 
            :b ["Hello" "World"]
            :c nil}]] 
    (is (= m (decode (encode m))))
    (is (= m (deserialize (serialize m))))))


(deftest test-map []
  (let [m {:a 1 :b "Hello"}] 
    (is (= m (deserialize (serialize m))))))

(deftest test-map []
  (let [m1 (mikera.persistent.IntMap/EMPTY)
        m2 (.include m1 2 "Hello")] 
    (is (= m1 (decode (encode m1))))
    (is (= m2 (decode (encode m2))))
    (is (= m1 (deserialize (serialize m1))))
    (is (= m2 (deserialize (serialize m2))))))


(deftest test-basic-types []
  (let [a (ic.map/point 1 2)
        b (ic.units/unit "Steam Tank")
        c (ic.map/terrain "Grassland")] 
    (is (= a (deserialize (serialize a))))
    (is (= b (decode (encode b))))
    (is (= {} (map-difference b (deserialize (serialize b)))))
    (is (= b (deserialize (serialize b))))
    (is (= c (deserialize (serialize c))))))


(deftest test-game []
  (let [g (ic.gamefactory/make-game)
        cg (assoc g :unit-locations nil)] 
    (is (= g (deserialize (serialize g))))))
