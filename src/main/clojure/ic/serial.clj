(ns ic.serial
  (:use [clojure.test])
  (:use [mc.util])
  (:use [ic protocols engine game units map gamefactory])
  (:import [mikera.util Tools Resource])
  (:use [clojure.data.json]))

(declare decode)
(declare encode-map)
(declare encode)

(defprotocol PSerial
  "Abstraction for serializable objects to strings"
  (encode [s]))

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

  mikera.persistent.LongMap 
    (encode [s] 
      ["IntMap"
       (reduce
         (fn [m [^Integer i v]]
           (assoc m (str i) (encode v)))
         {}
         s)])
    
    
  ic.engine.Unit  
    (encode [s]
      (let [uname (:name s)]
	      ["Unit"
	       uname
	       (encode-map (map-difference s (ic.units/unit-type-map uname)))]))

  ic.engine.Terrain  
    (encode [s]
      (let [tname (:name s)]
        ["Terrain"
         tname
         (encode-map (map-difference s (ic.map/terrain-type-map tname)))]))

  ic.engine.Player  
    (encode [s]
      ["Player"
       (encode-map s)])

  ic.engine.Game  
    (encode [s]
      ["Game"
       (encode-map s)])

    
  ic.engine.Point  
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
    
  clojure.lang.Keyword
    (encode [s]
      ["KW" (name s)])  
    
  java.lang.Integer
    (encode [s]
      ["I" s])  
    
  clojure.lang.IPersistentCollection
    (encode [s]
      (into (empty s) (map encode s)))  
  
  nil
    (encode [s]  
      nil)  
  
  java.lang.Long
    (encode [s] 
      s)
    
  java.lang.String
    (encode [s] 
      s)
    
  java.lang.Boolean
    (encode [s] 
      s)
    
  java.lang.Object
    (encode [s] 
      (error "Failure trying to encode: " s)))


(defn serialize-to-json [value]
  (json-str value))

(defn serialize [s]
  (serialize-to-json (encode s)))

(defn deserialize-from-json [string]
  (read-json string false)) ; read withoud keywordizing

;; keys in maps don't get encoded
(defn encode-map [s]
  (reduce 
    (fn [m [k v]]
      (assoc m k (encode v)))
    {}
    s))

(defn decode-unit [[dtype uname props]]
  (merge (ic.units/unit-type-map uname) (decode props)))

(defn decode-terrain [[dtype tname props]]
  (merge (ic.map/terrain-type-map tname) (decode props)))

(defn decode-player [[dtype props]]
  (merge (ic.engine/player (decode props))))

(defn decode-keyword [[dtype name]]
  (keyword name))

(defn decode-integer [[dtype val]]
  (int val))

(defn decode-game [[dtype props]]
  (ic.engine.Game. nil (decode props)))

(defn decode-point [[dtype x y]]
  (point x y))


(defn decode-sparsemap [[dtype tx ty w h rows]]
  (reduce-indexed 
    (fn [^mikera.persistent.SparseMap sm ^Integer iy row]
      (reduce-indexed
        (fn [^mikera.persistent.SparseMap sm ^Integer ix v]
          (mset sm (+ tx ix) (+ ty iy) (decode v)))
        sm
        row))
    (new-map)
    rows))

(defn decode-longmap [[dtype m]]
  (reduce
    (fn [^mikera.persistent.LongMap im [i v]]
      (.include im (Long/parseLong i) (decode v)))
    (mikera.persistent.LongMap/EMPTY)
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
            (decode-longmap data)
          (= "KW" dtype)
            (decode-keyword data)
          (= "I" dtype)
            (decode-integer data)
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

