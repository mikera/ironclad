(ns ic.engine
  "The main Ironclad game engine"
  (:require [ic.protocols :refer [PLocationSet]])
  (:require [mc util] :refer [get-points])
  (:require [clojure.set]))

(set! *warn-on-reflection* true)
(set! *unchecked-math* true)

;; ===============================================
;; constants

(def ^:const TERRAIN_SIZE 128)
(def ^:const HALF_TERRAIN_SIZE (long (/ TERRAIN_SIZE 2)))

;; ================================================
;; forward declarations
(declare adjacents)

;; ================================================
;; Locations and points


; Concrete type to implement a set of points
(deftype PointSet [^clojure.lang.IPersistentSet pointset]
  PLocationSet
    (get-points [p] pointset)
    (union [p q]  
      (reduce conj p (get-points q)))
    (intersection [p q] 
      (clojure.set/intersection pointset (get-points q)))
    (expand [p]
      (PointSet. (reduce clojure.set/union  pointset (map adjacents pointset))))
  clojure.lang.Seqable
    (seq [p] (seq  pointset))
  clojure.lang.ISeq
    (first [p] (first pointset))
    (next [p] (next pointset))
    (more [p] (rest pointset))
    (cons [p x] (cons x pointset)))

; Concrete type to represent a point location
(deftype Point [^long x ^long y]
  Object
    (toString [self] (str "(" x "," y ")"))
    (hashCode [self] (unchecked-add x (Integer/rotateRight y 16)))
    (equals [self b] 
            (if (instance? Point b) 
              (let [b ^Point b] (and (= x (.x b)) (= y (.y b)))) 
              false)))

(defn get-x ^long [^Point p] (.x p))

(defn get-y ^long [^Point p] (.y p)) 

(defn add ^Point [^Point p ^Point q] 
  (Point. (+ (.x p) (.x q)) (+ (.y p) (.y q))))

(defn adjacents [^Point p]
  (PointSet. 
    (areduce
      mikera.engine.Hex/HEX_DX 
      i 
      pts 
      #{} 
      (conj pts (Point. 
                (+ (.x p) (aget mikera.engine.Hex/HEX_DX i))
                (+ (.y p) (aget mikera.engine.Hex/HEX_DY i)))))))

(defn adjacent-point-list 
  ([^long x ^long y]
    (areduce
      mikera.engine.Hex/HEX_DX 
      i 
      pts 
      nil 
      (cons  
        (Point. 
                  (+ x (aget mikera.engine.Hex/HEX_DX i))
                  (+ y (aget mikera.engine.Hex/HEX_DY i))) 
        pts)))
  ([^Point p]
	  (areduce
		  mikera.engine.Hex/HEX_DX 
		  i 
		  pts 
		  nil 
		  (cons  
		    (Point. 
			            (+ (.x p) (aget mikera.engine.Hex/HEX_DX i))
			            (+ (.y p) (aget mikera.engine.Hex/HEX_DY i))) 
		    pts))))

(defn ^Point point 
  ([x y] 
    (Point. (int x) (int y)))
  ([[x y]] 
    (Point. (int x) (int y))))

(defn ^PointSet point-set 
  ([] 
    (PointSet. #{}))
  ([#^Point x] 
    (PointSet. #{x})))

; Direction function

(defn calc-dir 
  ([^Point p]
    (mikera.engine.Hex/direction (.x p) (.y p)))
    ([^Point s ^Point t]
    (mikera.engine.Hex/direction (- (.x t) (.x s)) (- (.y t) (.y s))))
  ([^Point p ^Integer tx ^Integer ty]
    (mikera.engine.Hex/direction (- tx (.x p)) (- ty (.y p))))
  ([^Integer sx ^Integer sy ^Integer tx ^Integer ty]
    (mikera.engine.Hex/direction (- tx sx) (- ty sx))))


; Map functions
 
(def EMPTY-MAP (mikera.persistent.SparseMap.)) 

(defn new-map  
  (^mikera.persistent.SparseMap []
    (do EMPTY-MAP)))

(defn new-units-map  
  (^mikera.persistent.SparseMap []
    (do EMPTY-MAP)))


(defn mget [^mikera.persistent.SparseMap m ^long x ^long y]
  (.get m (int x) (int y)))
	
(defn mset [^mikera.persistent.SparseMap m ^long x ^long y v]
  (.update m (int x) (int y) v))

(defn mvisit [^mikera.persistent.SparseMap m f]
  (.visit 
    m 
    (proxy [mikera.persistent.SparseMap$Visitor] []
      (call [x y value param]
        (f x y value)
        false))
    nil))

(defn mmap ^mikera.persistent.SparseMap [^mikera.persistent.SparseMap m f]
  (let [a (atom m)]
    (.visit 
      m 
      (proxy [mikera.persistent.SparseMap$Visitor] []
        (call [x y value a]
          (swap! a mset x y (f value))
          false))
      a)
    @a))

(defn mmap-indexed ^mikera.persistent.SparseMap [^mikera.persistent.SparseMap m f]
  (let [a (atom m)]
    (.visit 
      m 
      (proxy [mikera.persistent.SparseMap$Visitor] []
        (call [x y value a]
          (swap! a mset x y (f x y value))
          false))
      a)
    @a))

(defn random-point [^mikera.persistent.SparseMap m]
  (let [^mikera.math.Bounds4i bds (.getNonNullBounds m)]
    (loop [i 100]
      (let [x (mikera.util.Rand/range (.xmin bds) (.xmax bds))
            y (mikera.util.Rand/range (.ymin bds) (.ymax bds))
            v (mget m x y)]
        (if (nil? v)
          (if (> i 0) (recur (dec i)) nil)
          (point x y))))))


(defrecord Terrain []
  PDrawable
    (sourcex [t]
      (* (:ix t) TERRAIN_SIZE))
	  (sourcey [t]
      (* (:iy t) TERRAIN_SIZE))
	  (sourcew [t]
      TERRAIN_SIZE)
		(centrex [t]
		  HALF_TERRAIN_SIZE)
		(centrey [t]
		  HALF_TERRAIN_SIZE)
	  (sourceh [t]
      TERRAIN_SIZE)
	  (source-image [t]
      (:source-image t))
    (drawable-icon [t]
     (mikera.gui.BufferedImageIcon. (source-image t) (sourcex t) (sourcey t) (sourcew t) (sourceh t))))

;; ==================================================================
;; utility functions

;; ====================================================================
;; player data structure and functions

(defrecord Player []
    PValidatable
	   (validate [p]))

(def default-player-data 
  {:name "No name"
   :id "No player ID assigned"
   :is-human false
   :ai-controlled false
   :side 0
   :resources 1000})

(defn player 
  ([props]
	  (Player. nil 
	    (merge
	      default-player-data
	      props))))

;; =====================================================================
;; Unit data structure and functions

(def ^:const UNIT_SIZE 64)
(def ^:const SIDE_IMAGE_OFFSET 8)
(def ^:const HALF_UNIT_SIZE (/ UNIT_SIZE 2))
(def ^:const UNIT_ICON_CLIP 0)

(defrecord Unit []
  PDrawable
    (sourcex [u]
      (* (+ (+ (int (:ix u)) (if (:oriented-image u) (int (:dir u)) (int 0))) (int (* SIDE_IMAGE_OFFSET (:side u)))) (int UNIT_SIZE)))
    (sourcey [u]
      (* (long (:iy u)) UNIT_SIZE))
    (centrex [u]
      HALF_UNIT_SIZE)
    (centrey [u]
      HALF_UNIT_SIZE)
    (sourcew [u]
      UNIT_SIZE)
    (sourceh [u]
      UNIT_SIZE)
    (source-image [u]
      (:source-image u))
    (drawable-icon [u]
     (mikera.gui.BufferedImageIcon. 
			(source-image u) 
			(+ (sourcex u) UNIT_ICON_CLIP) 
			(+ (sourcey u) UNIT_ICON_CLIP) 
			(- (sourcew u) (* 2 UNIT_ICON_CLIP)) 
			(- (sourceh u) (* 2 UNIT_ICON_CLIP))))
   PValidatable
  		(validate [u]
			  (assert (>= (:aps u) 0))
			  (assert (instance? Long (:aps u)))
			  (assert (instance? Long (:hps u)))
			  (let [contents (:contents u)]
			    (assert (>= (count contents) 0))
			    (doseq [c contents]
			      (assert (nil? (:id c)))))
			  true))


;; ====================================================================
;; Game data structure and functions

(declare new-unit-id)
(declare get-terrain)
(declare set-terrain)
(declare get-unit)

(defrecord Game 
  []
  PGame
    (get-map [g]
      (:terrain g))

    (add-player [g p]
	    (let [players ^mikera.persistent.LongMap (:players g)
            pid (long (mikera.util.Rand/r 1000000))] 
        (if (nil? (.get players pid))    
          (assoc g :players (.include players pid (merge p {:id pid})))
          (recur p))))
    (update-player 
      [g p]
      "Updates the data for a specific player"
      (let [players ^mikera.persistent.LongMap (:players g)
            pid (long (:id p))]
        (assoc g :players (.include players pid p))))    
    (get-player [g player-id]
      (let [players ^mikera.persistent.LongMap (:players g)]
        (.get players player-id)))

    (get-unit-map [g]
      (:units g))
  PValidatable
	  (validate [g]
		  (let [^mikera.persistent.LongMap unit-locs (:unit-locations g)
		        ^mikera.persistent.SparseMap units (:units g)
		        players ^mikera.persistent.LongMap (:players g)]
			  (doseq [[uid ^ic.engine.Point loc] unit-locs]
			    (let [u (get-unit g (.x loc) (.y loc))
		            player-id (:player-id u)]
				    (assert (instance? ic.engine.Point loc))
				    (assert (= uid (:id u)))
			      (assert (not (nil? player-id)))
		        (assert (not (nil? (.get players player-id))))
            (validate u)))
			  (doseq [[pid player] (:players g)]
			    (let []
			      (assert (= pid (:id player)))
            (validate player))))
		  true))

(defn location-of-unit ^ic.engine.Point [^Game game unit-or-id]
  (if (instance? ic.engine.Unit unit-or-id)
    (.get ^mikera.persistent.LongMap (:unit-locations game) (long (:id unit-or-id)))
    (.get ^mikera.persistent.LongMap (:unit-locations game) (long unit-or-id))))

(defn remove-unit [g ^long x ^long y]
  (let [^PUnit u (mget (:units g) x y)
        uid (long (:id u))]
    (-> g
      (assoc :units (mset (:units g) x y nil))
      (assoc :unit-locations 
        (let [^mikera.persistent.LongMap ul (:unit-locations g)] 
          (.delete ul uid))))))
    
(defn get-unit 
  ([g ^long id]
    (if-let [^ic.engine.Point p (.get ^mikera.persistent.LongMap (:unit-locations g) (long id))]
      (get-unit g (.x p) (.y p))))
  ([g ^long x ^long y]
    (mget (:units g) x y)))

(defn add-unit [g ^long x ^long y u]
  (let [u ^Unit u
        current-id (:id u)
        uid (long (or current-id (new-unit-id g)))
        u-with-id (if current-id u (assoc u :id uid))]
    (-> g
      (assoc :units (mset (:units g) x y u-with-id))
      (assoc :unit-locations 
        (let [^mikera.persistent.LongMap ul (:unit-locations g)] 
          (.include ul uid (point x y)))))))

(defn get-terrain [g ^long x ^long y]
  (mget (:terrain g) x y))

(defn set-terrain [g ^long x ^long y t]
  (assoc g :terrain (mset (:terrain g) x y t)))


(defn new-unit-id [g]
  "Creates a new, unused unit ID for the given game"
  (let [^mikera.persistent.LongMap ul (:unit-locations g)
        id (mikera.util.Rand/nextLong)
        cu (.get ul id)]
    (if (nil? cu)
      id
      (recur g))))


(defn new-game [] 
  (Game.
    nil 
    {:terrain (new-map)
     :units (new-units-map)
     :effects (new-map)
     :visibility (new-map)
     :players (mikera.persistent.LongMap/EMPTY)
     :unit-locations (mikera.persistent.LongMap/EMPTY)
     :turn-number 1}))

;; ==========================================================