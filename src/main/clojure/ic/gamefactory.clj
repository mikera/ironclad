(ns ic.gamefactory
  (:use [ic.protocols])
  (:use [clojure.test])
  (:use [mc.util])
  (:import [mikera.engine Hex])
  (:import [mikera.util Rand])
  (:require [ic.map])
  (:require [ic.player])
  (:require [ic.game])
  (:require [ic.units]))

(def blank-map 
  (ic.map/new-map))

(def DEFAULT_MAP_SIZE 20)

(defn find-point [g pred] 
  (loop [i 1000]
    (let [^ic.map.Point pt (ic.map/random-point (:terrain g))]
      (if (pred (.x pt) (.y pt))
        pt
        (if (> i 0)
          (recur (dec i))
          nil)))))

(defn add-unit-random-position [g side u]
  (let [^ic.map.Point pt (find-point 
              g 
              (fn [x y] 
                (and 
                  (nil? (get-unit g x y))
                  (ic.units/suitable-terrain u (get-terrain g x y)))))
        new-unit (merge u 
                   {:side side
                    :player-id (:id (ic.game/get-player-for-side g side))})]
    (if (nil? pt)
      g
      (-> g 
        (add-unit (.x pt) (.y pt) new-unit)))))

(defn add-units [g]
  (-> g
    (add-unit-random-position 0 (ic.units/unit "Fortress Turret"))
    (add-unit-random-position 0 (ic.units/unit "Assault Zeppelin"))
    (add-unit-random-position 0 (ic.units/unit "Patrol Boat"))
    (add-unit-random-position 0 (ic.units/unit "Artillery Tank"))
    (add-unit-random-position 0 (ic.units/unit "Paddle Cruiser"))
    (add-unit-random-position 0 (ic.units/unit "Construction Crawler"))
    (add-unit-random-position 0 (ic.units/unit "Rifles"))
    (add-unit-random-position 0 (ic.units/unit "Steam Tank"))
    (add-unit-random-position 1 (ic.units/unit "Construction Crawler"))
    (add-unit-random-position 1 (ic.units/unit "Battle Tank"))
    (add-unit-random-position 1 (ic.units/unit "Rifles"))
    (add-unit-random-position 1 (ic.units/unit "Rifles"))
    (add-unit-random-position 1 (ic.units/unit "Rifles"))))

;(defn draw-terrain-line [g terrain-function sx sy tx ty]
;  (if (and (= sx tx) (= sy ty))
;    (set-terrain g tx ty (terrain-funtion (get-terrain g tx ty)))
;    (let [])))


(defn regularize-map 
  ([m]
    (regularize-map m 0.5))
  ([m ^Double prob]
	  (let [tm (atom m)] 
	    (mvisit m 
	      (fn [x y v]
	        (swap! tm 
	          (fn [om]
	            (let [dir (mikera.util.Rand/r 6)
	                  dt (mget m (+ x (Hex/dx dir)) (+ y (Hex/dy dir)))]
	              (if (and dt (Rand/chance prob))
	                (mset om x y dt)
	                om))))))
	    @tm)))

(defn subdivide-map
  ([m]
    (let [tm (atom m)] 
      (mvisit m 
        (fn [x y v]
          (let [cx (dec (* 2 x))
                cy (dec (* 2 y))]
	          (dotimes [ix 2]
	            (dotimes [iy 2]
		            (swap! tm mset 
		              (+ cx ix) 
		              (+ cy iy) 
		              (if 
	                  (Rand/chance 0.5)
	                  (if (= ix 1) (mget m (inc x) y) v)
	                  (if (= iy 1) (mget m x (inc y)) v)))))
           ; these two are needed to handle nil edge corners
	         (swap! tm mset (inc cx) (dec cy) 
             (if (Rand/chance 0.5) v (mget m (inc x) (dec y))))
           (swap! tm mset (dec cx) (inc cy) 
             (if (Rand/chance 0.5) v (mget m (dec x) (inc y)))))))
      @tm)))

(defn make-map-using-function 
  ([] (make-map-using-function 13))
  ([size]
    (make-map-using-function size (fn [x y] (ic.map/rand-terrain))))
  ([size function-xy] 
	  (let [m (ic.map/new-map)]
	    (reduce
	      (fn [m [x y]] (mset m x y (function-xy x y)))
	      m
	      (for [
	            x (range 1 (inc size)) 
	            y (range 1 (inc size))] [x (- y (unchecked-divide (int x) 2))])))))

(def region-defs
  [
   {:desc "Pure grasslands"
    :terrain-types ["Grassland"]}
   {:desc "Grasslands with scattered lakes and woods"
    :terrain-types ["Grassland" "Grassland" "Grassland" "Grassland" "Grassland" "Grassland" "Grassland" "Grassland" "Sea" "Wooded Grassland" "Sea"]}
   
   {:desc "Grasslands with scattered features"
    :terrain-types ["Grassland" "Grassland" "Grassland" "Grassland" "Grassland" "Grassland" "Grassland" "Grassland" "Woods" "Wooded Grassland" "Rocky Grassland"]}
   {:desc "Pure open sea"
    :terrain-types ["Deep Sea"]}
   {:desc "Deep Sea with features"
    :terrain-types ["Deep Sea" "Deep Sea" "Deep Sea" "Deep Sea" "Deep Sea" "Sea" "Sea Rocks" "Impassable Mountain"]}
   {:desc "Shallow Sea"
    :terrain-types ["Sea" "Sea" "Sea" "Sea" "Deep Sea" "Deep Sea" "Sea Rocks" "Grassland"]}
   {:desc "Rocky coastal/lakes area"
    :terrain-types ["Sea" "Sea" "Grassland" "Grassland" "Rocky Grassland" "Rocky Hills" "Mountain" "Deep Sea" "Sea Rocks"]}
   {:desc "Heavily wooded area"
    :terrain-types ["Woods" "Woods" "Woods" "Grassland" "Grassland" "Rocky Grassland" "Wooded Hills" "Wooded Grassland"]}
   {:desc "Hilly wooded/rocky area"
    :terrain-types ["Grassland" "Grassland" "Grassland" "Wooded Grassland" "Rocky Grassland" "Woods" "Rocky Hills" "Wooded Hills"  "Hills" "Mountain"]}
   {:desc "Mountainous area"
    :terrain-types ["Grassland" "Grassland" "Grassland" "Grassland" "Rocky Grassland" "Rocky Hills" "Wooded Hills" "Wooded Grassland" "Rocky Grassland" "Hills" "Hills" "Mountain" "Mountain" "Impassable Mountain"]}
  ])

(defn make-regions
  ([size]
    (applyn 
      subdivide-map
      3
      (make-map-using-function (+ 2 (/ size 8)) (fn [x y] (rand-choice region-defs))))))

(defn make-map-from-regions [region-map]
  (applyn 
    regularize-map
    2
    (mmap region-map 
      (fn [{terrain-types :terrain-types}]
        (ic.map/terrain (rand-choice terrain-types))))))

(defn cut-map
  ([source size]
    (make-map-using-function 
      size
      (fn [x y] (mget source x y)))))

(defn make-map
  ([] (make-map DEFAULT_MAP_SIZE))
  ([size]
    (let [source (make-map-from-regions (make-regions size))]
      (cut-map source size)
;      source
      )))

(defn add-default-players [game]
  (-> game
    (add-player 
      (ic.player/player 
        {:name "Albion"
         :side 0 
         :is-human true 
         :ai-controlled false}))
    (add-player 
      (ic.player/player 
        {:name "Krantz"
         :side 1 
         :ai-controlled true}))
    (add-player 
      (ic.player/player
        {:name "Mekkai"
         :side 2 
         :ai-controlled false}))
    (add-player 
      (ic.player/player 
        {:name "Rebels"
         :side 3 
         :ai-controlled true}))))

(defn random-terrain-game [size]
  (-> (ic.game/new-game) 
    (assoc :terrain (make-map size))
    (add-default-players)))

; random challenge game
(defn random-challenge-game []
  (-> (ic.game/new-game) 
    (assoc :terrain (make-map DEFAULT_MAP_SIZE))
    (add-default-players)
    (#(reduce 
	      (fn [g i]
          (-> g
		        (add-unit-random-position 0 (ic.units/random-unit))
		        (add-unit-random-position 1 (ic.units/random-unit))))
	      %1
        (range 10)))))

; game factory function

(defn make-game 
  ([] (make-game DEFAULT_MAP_SIZE))
  ([size] 
	  (-> (ic.game/new-game) 
	    (assoc :terrain (make-map size))
	    (add-default-players)
	    (add-units))))


(def test-map
  (let [m (ic.map/new-map)]
	  (reduce
	    (fn [m [x y]] (mset m x y (ic.map/terrain "Grassland")))
	    m
	    (for [
	          x (range 0 10) 
	          y (range 0 10)] [x y]))))

(defn test-game []
  (let [g (ic.game/new-game)] 
	  (-> g 
	    (assoc :terrain test-map)
	    (add-player 
	      (ic.player/player
	        {:side 0 
	         :is-human true 
	         :ai-controlled false})))))


(deftest t2
  (let [tg (test-game)
        u (ic.units/unit "Steam Tank" {:player-id (:id (ic.game/get-player-for-side tg 0))})
        g (-> tg
            (add-unit 2 2 u))]
    (is (not (nil? (get-unit g 2 2))))
    (is (not (nil? (first (:players g)))))
    (is (> (:aps u) 0 ))
    (is (= 1 (ic.units/move-cost g u 2 2 2 3)))
    (ic.game/validate-game g)))