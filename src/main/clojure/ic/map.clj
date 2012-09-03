(ns ic.map  
  "This file contains map data and terrain definitions"
  (:use [mc.util])
  (:use [ic protocols engine])
  (:require [ic.graphics])
  (:require [clojure.set]) 
  (:import [mikera.engine.Hex])
  (:import [mikera.math.Bounds4i])
  (:import [mikera.persistent.SparseMap])
  (:import [java.awt Color]))


(set! *warn-on-reflection* true)
(set! *unchecked-math* true)

(def TERRAIN_TYPES [
  (def TERRAINTYPE_OPEN "Open")
  (def TERRAINTYPE_WATER "Water")
  (def TERRAINTYPE_DEEP_WATER "Deep Water")
  (def TERRAINTYPE_WOODS "Woods")
  (def TERRAINTYPE_SAND "Sand")
  (def TERRAINTYPE_HILLS "Hills")
  (def TERRAINTYPE_MOUNTAIN "Mountain")
  (def TERRAINTYPE_URBAN "Urban")
  (def TERRAINTYPE_FORTRESS "Fortress")
  (def TERRAINTYPE_IMPASSABLE "Impassable")])


; Terrain functions


(def default-terrain-data 
  {:image ic.graphics/terrain-image
   :ix 0
   :move-cost-multiplier 100
   :defence-multiplier 100
   :map-colour (Color. 255 0 255)
   :iy 0
   :elevation 1
   :source-image ic.graphics/terrain-image})

; list of terrain types, built by make-terrain
(def temp-terrain-types (atom nil))

(defn make-terrain 
  ([props]
    (swap! temp-terrain-types conj
      (ic.engine.Terrain.
        nil
        (merge default-terrain-data props))))
  ([parent-name props] 
	  (make-terrain
		  (merge (find-first #(= parent-name (:name %)) @temp-terrain-types) props))))


;; ============================================== 
;; Open terrain

(make-terrain
    {:name "Grassland"
     :terrain-type TERRAINTYPE_OPEN
     :ix 1
     :iy 0
     :map-colour (Color. 50 100 20)
     :elevation 1})

(make-terrain "Grassland"
  {:name "Rocky Grassland"
   :ix 1
   :iy 1
   :move-cost-multiplier 110
   :defence-multiplier 110})

(make-terrain "Grassland"
  {:name "Wooded Grassland"
   :ix 1
   :iy 2
   :move-cost-multiplier 110
   :defence-multiplier 120})

; fortifications
(make-terrain
  {:name "Trench"
   :terrain-type TERRAINTYPE_FORTRESS
   :map-colour (Color. 90 60 30)
   :ix 3
   :iy 0
   :has-wire true})

;; =======================================
;; Hills and Mountains

(make-terrain
    {:name "Hills"
     :terrain-type TERRAINTYPE_HILLS
     :map-colour (Color. 100 60 30)
     :ix 4
     :iy 0})

(make-terrain "Hills"
    {:name "Rocky Hills"
     :ix 4
     :iy 1
     :move-cost-multiplier 110
     :defence-multiplier 130})

(make-terrain "Hills"
    {:name "Wooded Hills"
     :move-cost-multiplier 120
     :defence-multiplier 120
     :ix 4
     :iy 2})

(make-terrain
    {:name "Mountain"
     :map-colour (Color. 100 80 60)
     :terrain-type TERRAINTYPE_MOUNTAIN
     :ix 7
     :iy 0})


 (make-terrain "Mountain"
    {:name "Impassable Mountain"
     :terrain-type TERRAINTYPE_IMPASSABLE
     :ix 7
     :iy 1})

;; =======================================
;; Woods 
(make-terrain
    {:name "Woods"
     :map-colour (Color. 30 80 10)
     :terrain-type TERRAINTYPE_WOODS
     :ix 5
     :iy 0})

;; =======================================
;; Seas and rivers

(make-terrain
  {:name "Sea"
   :terrain-type TERRAINTYPE_WATER
   :map-colour (Color. 0 40 80)
   :is-water true
   :ix 2
   :iy 0
   :elevation 0})

(make-terrain "Sea"
  {:name "Sea Rocks"
   :terrain-type TERRAINTYPE_IMPASSABLE
   :move-cost-multiplier 0
   :is-water true
   :ix 2
   :iy 1})

(make-terrain "Sea"
  {:name "Deep Sea"
   :terrain-type TERRAINTYPE_DEEP_WATER
   :is-water true
   :ix 2
   :iy 2})

(def terrain-types
  @temp-terrain-types)

(def terrain-type-map 
   (reduce 
     (fn [m t] (assoc m (:name t) t)) 
     {} 
     terrain-types))

(defn terrain ^ic.engine.Terrain [tname] 
  (or 
    (terrain-type-map tname) 
    (throw (Error. (str  "Terrain type [" tname "] not found")))))


; Map builders

(defn rand-terrain []
  (terrain (rand-choice [
      "Impassable Mountain" "Mountain" "Rocky Hills" "Wooded Hills" "Trench" 
      "Hills" "Hills" "Hills" "Hills"
      "Grassland" "Grassland" "Grassland" "Grassland" "Grassland" "Grassland" 
      "Grassland" "Grassland" "Grassland" 
      "Rocky Grassland" "Wooded Grassland" "Woods" "Woods"
      "Sea" "Sea" "Sea" "Sea" "Sea" "Sea" "Sea"
      "Sea Rocks" "Deep Sea" "Deep Sea"])))
