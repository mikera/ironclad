(ns ic.units
  (:use [clojure.test])
  (:use [mc.util])
  (:use [ic protocols engine map graphics])
  (:use [ic.command])
  (:require [mc.csv])
  (:require [ic.lib])
  (:import [mikera.engine Hex])
  (:import [mikera.util Rand])
  (:import [java.awt Color]))

(set! *unchecked-math* true)
(set! *warn-on-reflection* true)

(declare unit)
(declare unit-type-map)



(def ^:const ATTACK_POWER_FACTOR 2.0)

(def ^:const AI_AGGRESSION_FACTOR 1.5) ; ratio of favouring attack / ignoring defence
(def ^:const AI_RANDOM_MOVE_FACTOR 1.00) ; scale of random noise to move evaluation

(def ^:const AI_OBJECTIVE_FACTOR 0.3) ; proportion of value for moving closer to enemy
(def ^:const AI_RISK_AVERSION_FACTOR 0.5) ; weighting for avoiding threats for risk-averse units


(def ^:const DEFAULT_RETURN_FIRE false)

(def UNIT_TYPES [
	(def UNITTYPE_INFANTRY "Infantry")
	(def UNITTYPE_LIGHT_VEHICLE "Light vehicle")
  (def UNITTYPE_MEDIUM_ARMOUR "Medium armour")
	(def UNITTYPE_HEAVY_ARMOUR "Heavy armour")
	(def UNITTYPE_AIRCRAFT "Aircraft")
	(def UNITTYPE_CAVALRY "Cavalry")
	(def UNITTYPE_SHIP "Ship")
	(def UNITTYPE_SUBMARINE "Submarine")
  (def UNITTYPE_EMPLACEMENT "Emplacement")
	(def UNITTYPE_BUILDING "Building")
	(def UNITTYPE_INFRASTRUCTURE "Infrastructure")])

(def MOVE_TYPES [
  (def MOVETYPE_TANK "Tracked")
	(def MOVETYPE_ROAD_VEHICLE "Road")
  (def MOVETYPE_OFFROAD_VEHICLE "Offroad")
	(def MOVETYPE_INFANTRY "Foot")
  (def MOVETYPE_CAVALRY "Cavalry")
  (def MOVETYPE_WALKER "Walker")
  (def MOVETYPE_HOVER "Hover")
  (def MOVETYPE_FLY "Fly")
  (def MOVETYPE_RAIL "Rail")
  (def MOVETYPE_SHIP "Ship")
  (def MOVETYPE_DEEP_SHIP "Deep Ship")
  (def MOVETYPE_IMMOBILE "Immobile")])

(def ATTACK_TYPES [
	(def ATTACKTYPE_SMALL "Small arms")
	(def ATTACKTYPE_DEATH_RAY "Death ray")
	(def ATTACKTYPE_LIGHT_CANNON "Light cannon")
	(def ATTACKTYPE_HEAVY_CANNON "Heavy cannon")
	(def ATTACKTYPE_ARTILLERY "Artillery")
	(def ATTACKTYPE_MELEE "Melee")
	(def ATTACKTYPE_EXPLOSIVE "Explosive")
	(def ATTACKTYPE_ROCKET "Rocket")
	(def ATTACKTYPE_MISSILE "Missile")
  (def ATTACKTYPE_TORPEDO "Torpedo")
  (def ATTACKTYPE_FLAK_CANNON "Flak cannon")])

; Load a CSV file in a nested map
; using the first colum as string keys for the outer map
; using the first row as string keys for the inner maps
(defn load-table [fname] 
  (let [reader (mc.csv/get-csv-reader fname)
        data (.readAll reader)
        labels (rest (first data))]
    (reduce
      (fn [m [attack-type & rest]]
        (assoc m attack-type 
					(zipmap 
	          labels 
	          (map (fn [^String s] (Integer/parseInt s)) rest))))
      {}
      (rest data))))

(def TERRAIN_DEFENCE (load-table "tables/terrain_defence.csv"))

(def TARGET_EFFECT (load-table "tables/target_effect.csv"))

(def TERRAIN_MOVE_COST (load-table "tables/terrain_move_cost.csv"))
 

; commun unit functions


(defn allied-unit? [u1 u2]
  (= (:side u1) (:side u2)))

(defn used-capacity [tu]
  (reduce 
    (fn [val u] (+ val (:size u))) 
    0 
    (:contents tu)))

(defn available-capacity [tu]
  (- (:capacity tu) (used-capacity tu)))


; unit entering logic




(defn can-enter? [unit tu]
  "Returns true if unit can enter another unit"
  (and 
    (:allow-enter tu)
    (<= (:size unit) (:max-contents-size tu))
    (if (allied-unit? unit tu) 
      (<= (:size unit) (available-capacity tu))
      (and (:can-capture unit) (:allow-capture tu)))))

(defn enter-unit [game unit tx ty tu]
  "Returns updates for a unit entering another unit. Excludes movement/removal of unit."
  (list-not-nil 
    (msg-update-unit tu
                  {:side (:side unit)
                   :player-id (:player-id unit)
                   :contents (conj (:contents tu)
                               (merge unit {:aps 0}))})))


; movement logic

(defn allowable-move-endpoint? [game unit tx ty]
  "Returns true if the destination is an allowable move end point - either empty or a unit that can be entered"
  (let [units (get-unit-map game)
        tu (mget units tx ty)] 
    (if (nil? tu)
      true
      (can-enter? unit tu)))) 
 
(defn base-move-cost ^long [unit terrain]  
  "Gets base move cost as percentage, 100 = 1 AP"
  (let [terrain-type (:terrain-type terrain)
        unit-move-type (:move-type unit)
        base-cost (long ((TERRAIN_MOVE_COST terrain-type) unit-move-type))]
    base-cost))

(defn ^Integer zoc-cost 
  ([game unit ^Integer sx ^Integer sy ^Integer tx ^Integer ty]  
    (let [move-dir (mikera.engine.Hex/direction sx sy tx ty)]
      (unchecked-add 
        ^Integer (zoc-cost game unit 
          (get-unit game (unchecked-add sx (mikera.engine.Hex/dx (inc move-dir))) (unchecked-add sy (mikera.engine.Hex/dy (inc  move-dir)))))
        ^Integer (zoc-cost game unit 
          (get-unit game (unchecked-add sx (mikera.engine.Hex/dx (dec move-dir))) (unchecked-add sy (mikera.engine.Hex/dy (dec  move-dir))))))))
  ([game unit enemy] 
    (if (or 
          (nil? enemy) 
          (<= (:zoc-size enemy) 0) 
          (allied-unit? unit enemy)
          (:ignore-zoc unit))
      0
      1)))

(defn move-cost [game unit ^Integer sx ^Integer sy ^Integer tx ^Integer ty]
  (let [terrain (get-terrain game tx ty)
        tu (get-unit game tx ty)
        unit-move-type (:move-type unit)]
    (double (cond 
      (nil? terrain)
        0
      (= unit-move-type MOVETYPE_FLY)
        1
      (and (not (nil? tu)) (not (allied-unit? unit tu))) 
        ; can only move through allied units. Capture case handled in move-improvement-list
        0
      ; check rail first
      (and (:has-rail terrain) (:has-rail (get-terrain game sx sy)) (> ((TERRAIN_MOVE_COST "Rail") unit-move-type) 0))
        (/ (int ((TERRAIN_MOVE_COST "Rail") unit-move-type)) 100.0)
      (and (:has-road terrain) (:has-road (get-terrain game sx sy)))
        (/ (int ((TERRAIN_MOVE_COST "Road") unit-move-type)) 100.0)
      :default
        (let [base-cost (base-move-cost unit terrain) 
              multiplier (int (:move-cost-multiplier terrain))]
	        (if (<= base-cost 0)
	          0
		        (+
		          (/ (* base-cost multiplier) 10000)
		          (zoc-cost game unit sx sy tx ty))))))))

(defn suitable-terrain [unit terrain]
  (or 
    (> (base-move-cost unit terrain) 0)
    (and (:has-rail terrain) (= (:move-type unit) MOVETYPE_RAIL))))

(defn move-improvement-list 
  ([game unit aps pos a moves]
    (move-improvement-list game unit aps pos a moves 0 []))
  ([game unit aps pos a moves i list]
    (if (>= i 6) 
       list
       (let [sx (get-x pos)
             sy (get-y pos)
             tx (+ sx (aget mikera.engine.Hex/HEX_DX i))
             ty (+ sy (aget mikera.engine.Hex/HEX_DY i))
             mcost1 (move-cost game unit sx sy tx ty)
             mcost (if (> mcost1 0) 
                     mcost1
                     (let [tu (get-unit game tx ty)]
                       (if (and tu (can-enter? unit tu))
                         (- aps a) ; use all remaining aps
                         0)))
             newaps (+ a mcost)
             newpos (point tx ty)
             bestaps (moves newpos)
             found (and 
                     (> mcost 0) ; move is allowable
                     (<= newaps aps) ; move within ap limit
                     (or (nil? bestaps) (< newaps bestaps))) ; move is best so far
             newlist (if found (conj list [newpos newaps]) list)]
         (do
;           (println i ": npos=" newpos "cost=" mcost " newaps=" newaps " bestaps=" bestaps " found=" found)
           (recur game unit aps pos a moves (inc i) newlist))))))

(defn merge-move-improvements [movemap newlist]
  (reduce
    (fn [mm [pos aps]] 
      (let [caps (mm pos)]
        (if (or (nil? caps) (< aps caps))
          (assoc mm pos aps)
          mm)))
    movemap
    newlist))

(defn move-improvements [game unit apsmax newmoves moves]
  (do 
;    (println newmoves " - apsmax=" apsmax)
    (reduce 
	    (fn [ms [pos a]] 
        (let [improvedlist (move-improvement-list game unit apsmax pos a moves)]
          (do 
;            (println "pos=" pos " : Move improvements=" improvedlist)
            (merge-move-improvements ms improvedlist)))) 
	    {}
	    newmoves)))

; gets map of all possible move destination points to ap cost
(defn possible-moves 
  ([game unit x y]
	  (let [aps (:aps unit)
	        moves {(point x y) 0}]
	    (possible-moves game unit aps moves {})))
  ([game unit apsmax newmoves allmoves]
    (if (empty? newmoves)
      allmoves
      (let [moves (merge allmoves newmoves)]
        (recur game unit apsmax (move-improvements game unit apsmax newmoves moves) moves)))))

; gets map of all allowable move destination points to ap cost
(defn allowable-moves [game unit x y]
  (reduce
    (fn [am [pos ap]] 
      (if (allowable-move-endpoint? game unit (get-x pos) (get-y pos))
        (assoc am pos ap)
        am))
    {}
    (possible-moves game unit x y)))


(defn trace-moves [possible-moves sx sy tx ty]
  (let [p (point tx ty)
        paps (possible-moves p)] 
    (cond 
      (nil? paps)
        (throw (Error. (str "Unable to trace: destination " p " not reachable!")))
      (and (= sx (.x p)) (= sy (.y p)))
        (list p)
      :default  
        (let [adjs (adjacent-point-list p)
	            pp ^ic.engine.Point (argmax #(- (or (possible-moves %) 1000000)) adjs  )]
	        (cons
	          p
	          (trace-moves possible-moves sx sy (.x pp) (.y pp)))))))





; deploy logic

(defn can-deploy? [ability game unit tx ty]
  (let [t (get-terrain game tx ty)
        tu (get-unit game tx ty)
        contents (:contents unit)]
    (and
      t
      (nil? tu)
      (some 
        (fn [du] 
          (and (> (:aps du 0)) (suitable-terrain du t))) 
        contents))))

(defn deploy-targets [ability game unit x y]
  (let []
	  (reduce 
	    (fn [bm ^ic.engine.Point pos] 
	      (if 
          (can-deploy? ability game unit (.x pos) (.y pos))
	        (assoc bm pos (:cost ability))
	        bm))
	    {}
	    (adjacent-point-list x y))))


; building logic
(defn suitable-build-terrain [build-unit terrain]
  (cond 
    (= MOVETYPE_FLY (:move-type build-unit))
      (= ic.map/TERRAINTYPE_OPEN (:terrain-type terrain)) ;can only build air units on open ground
    (= MOVETYPE_IMMOBILE (:move-type build-unit))
      (if (or (:has-road terrain) (:has-rail terrain))
        false
        (suitable-terrain build-unit terrain))
    :default
      (suitable-terrain build-unit terrain)))

(defn buildable-unit-names [ability game unit tx ty]
  (let [t (get-terrain game tx ty)]
    (or
	    (and
	      (not (nil? t))
	      (nil? (get-unit game tx ty))
	      (seq (filter 
	        (fn [unit-name] 
	          (let [build-unit (unit-type-map unit-name)]
	            (and 
	              (>= 
	                (:resources (get-player game (:player-id unit))) 
	                (:value build-unit))
	              (suitable-build-terrain build-unit t)))) 
	        (:build-list ability))))
       nil))) ; default to nil return value if no units possible

(defn can-build? [ability game unit tx ty]
  (not (empty? (buildable-unit-names ability game unit tx ty))))

(defn build-targets [ability game unit x y]
  (reduce 
    (fn [bm ^ic.engine.Point pos] 
      (if (can-build? ability game unit (.x pos) (.y pos))
        (assoc bm pos (:cost ability))
        bm))
    {}
    (adjacent-point-list x y)))


; attack logic

(defn attack-cost-aps [ability unit]
  (max 
    (:cost ability)
    (if (:consume-all-aps ability)
      (:aps unit)
      0)))

(defn ^Double attack-power [ability unit target]
  (* 
    (double (:power ability))
    (double ((TARGET_EFFECT (:attack-type ability)) (:unit-type target)))))

(defn ^Double defence-terrain-modifier [target terrain]
  (let [target-type (:unit-type target)]	 
    (cond
	    (= UNITTYPE_AIRCRAFT target-type)
        100.0
	    :default
	      (double
			    (max
			      1 
			      (* 
			        ((TERRAIN_DEFENCE (:terrain-type terrain)) (:unit-type target))
			        (:defence-multiplier terrain)
			        0.01))))))

(defn ^Double defence-power [ability unit target terrain]
  (*  
    (double (:armour target))
    (defence-terrain-modifier target terrain)))

(defn calc-damage [ability unit target game tx ty]
  (*
    ATTACK_POWER_FACTOR
    (attack-power ability unit target)
    (/ (defence-power ability unit target (get-terrain game tx ty )))))

(defn ^Integer round-damage [dam]
  (mikera.util.Rand/round (double dam)))

(defn damage-unit [target game tx ty ^Integer dam]
  (let [^Integer hps (:hps target)]	
    (cond 
	    (>= dam hps)
	      (list 
	        (merge (msg-remove-unit target tx ty)
            {:animation {:animation-name "Explosion"
                         :tx tx
                         :ty ty}}))
	    (> dam 0)
		    (list 
		      (merge  
		        (msg-update-unit target 
		            {:hps (unchecked-subtract hps dam)})
	          {:animation {:animation-name "Damage"
	                       :damage dam
	                       :tx tx
	                       :ty ty
                         :iy 6}}))
	    :default
	      (list ))))

(defn try-fire [ ability unit target game sx sy tx ty aps apcost]
  "Return list of updates to represent unit fire on target"
  (let [estimated-damage (calc-damage ability unit target game tx ty)
        actual-damage (round-damage estimated-damage)
        new-aps (- aps apcost)]
    (concat
      (list
        (merge
	        (msg-update-unit unit 
	          {:aps new-aps})
          {:animation {:animation-name "Shoot"
                       :ability-name (:name ability)
                       :sx sx
                       :sy sy
                       :tx tx
                       :ty ty}}
          (if (not= MOVETYPE_RAIL (:move-type unit) )
            {:dir (calc-dir (location-of-unit game unit) tx ty)}
            {})))
      (damage-unit target game tx ty actual-damage))))

(defn can-attack [ability unit target game sx sy tx ty]
  "Return true if unit is able to attack a target with the given ability"
  (let [distance (mikera.engine.Hex/distance sx sy tx ty)]	
    (and 
	    (<= (:min-range ability) distance)
	    (>= (:max-range ability) distance)
	    (not (allied-unit? unit target))
	    (>= (:aps unit) (attack-cost-aps ability unit))
	    (> (attack-power ability unit target) 0))))



(defn get-best-attack [unit target game sx sy tx ty]
  "Gets the most efective attack ability for a unit against a given target, nil if not possible"
  (let [abs (:abilities unit)]
    (reduce
      (fn [ab newab] 
        (if 
          (and
            (:is-attack newab)
            (can-attack newab unit target game sx sy tx ty)
            (or 
              (nil? ab)
              (>= (calc-damage newab unit target game tx ty) (calc-damage newab unit target game tx ty))))
          newab
          ab))
      nil
      abs)))

(defn try-return-fire [ unit target game sx sy tx ty]
  (if (:return-fire unit)
    (let [ability (get-best-attack unit target game sx sy tx ty)]
      (if (nil? ability)
        (list )
        (let [apcost (attack-cost-aps ability unit)
              aps (:aps unit)]
          (try-fire ability unit target game sx sy tx ty aps apcost))))))


(defn allowable-attacks [game unit ability x y]
  "Return map of attackable points to attack ap cost"
  (let [range (:max-range ability)
        rangesize (+ range 1 range)
        attacks (atom  {})
        cost (attack-cost-aps ability unit)]

    (dotimes [iy rangesize]
      (dotimes [ix rangesize]
	      (let [tx (+ x ix (- range))
	            ty (+ y iy (- range))
	            tu (get-unit game tx ty)]
	        (if
	          (and 
              (not (nil? tu))
              (can-attack ability unit tu game x y tx ty)
	            (not (and (= x tx) (= y ty)))
	            (<= cost (:aps unit)))
	          (swap! attacks assoc (point tx ty) cost)))))
	    @attacks))

(defn ai-unit-value ^double [game unit x y]
  (* 1.0 (:value unit)))

; ======================================================================================================================
; Ability implementations

; Move ability

(defn ai-threat-level-for-hex [game unit threat-map x y]
  "AI threat level, 1.0=high threat"
  (/
    (min 100 (mget threat-map x y))
    (defence-terrain-modifier unit (get-terrain game x y))))

(defrecord MoveAbility []
  PAbility
	  (get-targets [ability game unit x y]
	    (allowable-moves game unit x y))
    (ability-icon [ability]
      2)
    (ai-evaluate [ability game unit sx sy tx ty apcost]
      (let [tu (get-unit game tx ty)] 
        (if tu
          (if (not (= (:side unit) (:side tu)))
            (:enter-value tu) ; capture value
            0) ; avoid moving into own buildings
          (let [rand-factor (* AI_RANDOM_MOVE_FACTOR (mikera.util.Rand/nextDouble))
                player (get-player game (:player-id unit))
                objective-map (:objective-value (:ai-evaluation player))
                threat-map (:enemy-threat-level (:ai-evaluation player))]
            (+ rand-factor 
              (if objective-map 
                (* 
                  (min 10 (- (mget objective-map tx ty) (mget objective-map sx sy)))
                  (- 1 (:risk-aversion unit))
                  AI_OBJECTIVE_FACTOR) 
                0)
              (if threat-map 
                (* -1.0
                  (- (ai-threat-level-for-hex game unit threat-map tx ty) (ai-threat-level-for-hex game unit threat-map sx sy)) 
                  (:value unit) 
                  (:risk-aversion unit) 
                  AI_RISK_AVERSION_FACTOR) 
                0))))))
    (needs-param? [ability] false)
    (ai-action-param [ability game unit sx sy tx ty]
      nil)
    (apply-to-game [ability game unit sx sy tx ty command]
      (let [possible-moves (possible-moves game unit sx sy)
            tu (get-unit game tx ty)
            tpos (point tx ty)
            ^Integer aps (:aps unit)
            move-aps (mikera.util.Maths/roundUp (double (or (possible-moves tpos) (inc aps))))]      
        (cond
          (nil? move-aps)
            (list (msg-command-fail (str "Unable to move to location: " tx "," ty ":::" possible-moves)))
          (< aps move-aps)
            (list 
              (msg-command-fail (str "Insufficient APS to move: (" sx "," sy ") -> (" tx "," ty ") aps=" aps "  apcost=" move-aps))
              (msg-update-unit unit :aps 0)) ; debug only
          (not (allowable-move-endpoint? game unit tx ty))
            (list (msg-command-fail (str "Move destination blocked")))
          
          ; enter unit / capture building
          (and tu (can-enter? unit tu))
            (let [moves (reverse (trace-moves possible-moves sx sy tx ty))]
	            (cons 
	              (merge 
	                (msg-remove-unit unit sx sy)
	                {:animation {:animation-name "Move" :animation-moves moves}})
	              (enter-unit game unit tx ty tu)))
          
          ; standard move  
          :default
	          (let [moves (reverse (trace-moves possible-moves sx sy tx ty))
                  [^ic.engine.Point lm1 ^ic.engine.Point lm2] (drop (- (count moves) 2) moves)]
		          (list-not-nil
		            (msg-update-unit unit :dir (mikera.engine.Hex/direction (- (.x lm2) (.x lm1)) (- (.y lm2) (.y lm1))))
	              (msg-update-unit unit :aps (int (- aps move-aps)))
	              (merge 
	                (msg-move-unit unit sx sy tx ty)
	                {:animation {:animation-name "Move" :animation-moves moves}}))))))

    (nameString [ability unit] 
      (str "Move: " (:class ability))))


; Deploy Ability


(defrecord DeployAbility []
  PAbility
    (get-targets [ability game unit x y]
      (if (>= (:aps unit) (:cost ability))
        (deploy-targets ability game unit x y)
        {}))
    (ability-icon [ability]
      5)
    (ai-evaluate [ability game unit sx sy tx ty apcost]
      (let [value (+ 1000 (Rand/nextDouble))]
;        (println value)
        value))
    (needs-param? [ability] true) ; param is position in contents list
    (ai-action-param [ability game unit sx sy tx ty]
      (let [terrain (get-terrain game tx ty)
            contents (:contents unit)
            deployables (filter (fn [du] (and (> (:aps du) 0) (suitable-terrain du terrain))) contents)]
        (if (not (empty? deployables))
          (let [du (rand-choice deployables)
                i (find-position contents du)]
            i)
          nil)))
    (apply-to-game [ability game unit sx sy tx ty command]
      (let [tpos (point tx ty)
            aps (:aps unit)
            deploy-terrain (get-terrain game tx ty)
            deploy-pos (int (:param command))
            deploy-aps-cost (if (:consume-all-aps ability) aps (:cost ability))           
            contents (:contents unit)
            player (get-player game (:player-id unit))
            deploy-unit (nth contents deploy-pos nil)]
        (cond
          (nil? deploy-unit)
            (list (msg-command-fail (str "Unable to find unit at index: " deploy-pos)))
          (< aps deploy-aps-cost)
            (list (msg-command-fail (str "Insufficient APS to deploy: " (:name deploy-unit))))
;          (<= (:aps deploy-unit) 0)
;            (list (msg-command-fail (str "Trying to deploy unit with zero APs: " (:name deploy-unit))))
          (not (nil? (get-unit game tx ty))) 
            (list (msg-command-fail (str "Deploy target area blocked")))
          (> 1 (mikera.engine.Hex/distance sx sy tx ty)) 
            (list (msg-command-fail (str "Cannot deploy in non-adjacent hex")))
          (not (suitable-terrain deploy-unit deploy-terrain))
            (list (msg-command-fail (str "Cannot deploy " (:name deploy-unit) " on this terrain: " (:name deploy-terrain))))
          :default
            (let [new-unit (merge deploy-unit
                             {:aps 0
                              :id nil ; important so this is re-added!
                              :dir (mikera.engine.Hex/direction sx sy tx ty)})]
	            (list 
	              (msg-update-unit unit 
	                 {:aps (- aps deploy-aps-cost)
	                  :contents (remove-nth contents deploy-pos)})          
	              (msg-add-unit new-unit tx ty) )))))
    (nameString [ability unit] 
      (str "Deploy contained unit [" (count (:contents unit)) "]")))



; Build Ability


(defrecord BuildAbility []
  PAbility
    (get-targets [ability game unit x y]
      (if (>= (:aps unit) (:cost ability))
        (build-targets ability game unit x y)
        {}))
    (ability-icon [ability]
      4)
    (ai-evaluate [ability game unit sx sy tx ty apcost]
      (if (> (:resources (get-player game (:player-id unit))) 300)
        (+ 20 (Rand/nextDouble))
        0))
    (needs-param? [ability] (not (:auto-choose-build ability)))
    (ai-action-param [ability game unit sx sy tx ty]
      (let [terrain (get-terrain game tx ty)
            build-list (filter #(suitable-build-terrain (unit-type-map %) terrain) (:build-list ability))]
        (if (not (empty? build-list))
          (mc.util/rand-choice build-list)
          nil)))
    (apply-to-game [ability game unit sx sy tx ty command]
      (let [tpos (point tx ty)
            aps (:aps unit)
            build-terrain (get-terrain game tx ty)
            build-list (:build-list ability)
            build-name (or (:param command) (first build-list))
            build-aps-cost (if (:consume-all-aps ability) aps (:cost ability))           
            player (get-player game (:player-id unit))
            new-unit (merge
		                   (ic.units/unit build-name)
			                   {:aps 0
                          :side (:side unit)
                          :player-id (:player-id unit)
			                    :dir (mikera.engine.Hex/direction sx sy tx ty)})
            build-resource-cost (:value new-unit)]
		    (cond
          (< (:resources player) build-resource-cost)
            (list (msg-command-fail (str "Insufficient resources (" (:resources player) ") to build: " build-name)))
          (< aps build-aps-cost)
            (list (msg-command-fail (str "Insufficient APS to build: " build-name)))
          (not (nil? (get-unit game tx ty))) 
            (list (msg-command-fail (str "Build target area blocked")))
          (> 1 (mikera.engine.Hex/distance sx sy tx ty)) 
            (list (msg-command-fail (str "Cannot build in non-adjacent hex")))
          (not (list-contains? build-list build-name))
            (list (msg-command-fail (str "Unable to build unit of type: " build-name)))
          (not (suitable-build-terrain new-unit build-terrain))
            (list (msg-command-fail (str "Cannot build " build-name " on this terrain: " (:name build-terrain))))
          :default
		        (list 
              (msg-update-player player 
                {:resources (- (:resources player) build-resource-cost)})
		          (msg-update-unit unit 
		             {:aps (- aps build-aps-cost)
                  :dir (mikera.engine.Hex/direction sx sy tx ty)})          
		          (msg-add-unit new-unit tx ty) ))))
    (nameString [ability unit] 
      (str "Build: " (:name ability))))


; Attack Ability

(defrecord AttackAbility []
  PAbility
    (get-targets [ability game unit  x y]
      (let [aa (allowable-attacks game unit ability x y)]
        ;(println aa)
        aa))
    (ability-icon [ability]
      1)
    (ai-evaluate [ability game unit sx sy tx ty apcost]
      (let [target (get-unit game tx ty)        
            apcost (attack-cost-aps ability unit)
            aps (:aps unit)
            dam (calc-damage ability unit target game tx ty)
            return-fire (if (:return-fire target) (get-best-attack target unit game tx ty sx sx) nil)
            return-dam (if (nil? return-fire)  0 (calc-damage return-fire target unit game sx sy))]
       (- 
         (* (ai-unit-value game target tx ty) (/ dam (:hpsmax target)) AI_AGGRESSION_FACTOR) 
         (* (ai-unit-value game unit sx sy) (/ return-dam (:hpsmax unit))))))
    (needs-param? [ability] false)
    (ai-action-param [ability game unit sx sy tx ty]
      nil)
    (apply-to-game [ability game unit sx sy tx ty command]
      (let [target (get-unit game tx ty)
            apcost (attack-cost-aps ability unit)
            aps (:aps unit)
            distance (mikera.engine.Hex/distance sx sy tx ty)]
	      (cond
          (nil? target)
            (list (msg-command-fail "Target not available")) 
	        (< (:max-range ability) distance)
	          (list (msg-command-fail "Target out of range"))
          (> (:min-range ability) distance)
            (list (msg-command-fail "Target too close"))
          (> apcost aps)
            (list (msg-command-fail "Insufficient APs to attack"))
	        :default
            (concat
              (try-fire ability unit target game sx sy tx ty aps apcost)
              (try-return-fire target unit game tx ty sx sy)))))
    (nameString [ability unit] 
      (str 
        (:name ability) "\n"
        "  - Action point cost: " (:cost ability) (if (:consume-all-aps ability) "+" "") "\n"
        "  - Power: " (:power ability) "\n" 
        "  - Range: " (:min-range ability) "-" (:max-range ability))))

; AI overall functions

(defn get-ability-targets [game unit x y]
  "Gets a map of abilities to {map of targets to ap cost}"
  (let [abs (:abilities unit)]
    (zipmap 
      abs 
      (map 
        (fn [ability] (get-targets ability game unit x y)) 
        abs))))

(defn ai-evaluate-best-attack [game unit sx sy aps-left]
  "Values the most efective attack ability for a unit (after moving)"
  (let [abs (:abilities unit)]
    (second
	    (reduce
	      (fn [[best-ability best-value :as best] ability] 
	        (if 
	          (and
	            (:is-attack ability)
	            (>= aps-left (:cost ability)))
            ; note this is the right way to check cost, apcost could be higher than aps-left due to :consume-all-aps
	 
	          (let [targets (get-targets ability game unit sx sy)
	                value-atom (atom 0)]
	            (doseq [[^ic.engine.Point pos apcost] targets]
	              (let [tx (.x pos)
                      ty (.y pos)] 
                  (let [v (ai-evaluate ability game unit sx sy tx ty apcost)]
                    (if (> v @value-atom) 
                      (reset! value-atom v)))))
	            (if (> @value-atom best-value) 
	              [ability @value-atom]
	              best))
	          best))
	      [nil 0]
	      abs))))

(defn ai-evaluate-end-position [game unit x y aps-left]
  (max
    0
    (+ (ai-evaluate-best-attack game unit x y aps-left) (- AI_RANDOM_MOVE_FACTOR))))

(defn ai-best-command [game unit x y]
  (let [targets (ic.units/get-ability-targets game unit x y)
        aps (:aps unit)
        cmd (mc.util/argmax 
              (fn [[value ability pos]]
                value)
              (mapcat 
                (fn [[ability ts]]
                  (map
                    (fn [[^ic.engine.Point pos apscost]]
                      [(+
                        (ai-evaluate ability game unit x y (.x pos) (.y pos) apscost)
                        (if (:is-move ability)
                          (ai-evaluate-end-position game unit (.x pos) (.y pos) (- aps apscost))
                          (ai-evaluate-end-position game unit x y (- aps apscost)))) 
                       ability 
                       pos])
                    ts))
                targets)
              0 ; initial best value
              nil)]
    (and cmd
      (let [[value ability ^ic.engine.Point pos] cmd
            tx (.x pos)
            ty (.y pos)
            param (ai-action-param ability game unit x y tx ty)]
        (if 
          (or param (not (needs-param? ability)))     
	        (ic.command/command 
	          (:id unit) 
	          (:name ability) 
	          tx 
	          ty
	          param)
          nil)))))



; move ability generator

(defn make-move-ability [move-class]
  (merge (MoveAbility.)
    {:name (str "Move: " move-class)
     :is-move true
     :class move-class
     :ability-colour java.awt.Color/CYAN}))

; attack ability generator

(def attack-sound-map
  {ATTACKTYPE_SMALL "Rifle fire"
   ATTACKTYPE_LIGHT_CANNON "Rifle fire"
   ATTACKTYPE_DEATH_RAY "Death ray fire"
   ATTACKTYPE_EXPLOSIVE "Explosion"})

(defn make-attack-ability [attack-type]
  (merge (AttackAbility.)
    attack-type
    {:is-attack true
     :min-range (or (:min-range attack-type) 1)
     :ability-colour java.awt.Color/RED
     :sound (or (attack-sound-map (:attack-type attack-type)) "Cannon fire")}))

; build ability generator

(defn make-build-ability [build-type]
  (merge (BuildAbility.)
    build-type
    {:is-build true
     :ability-colour java.awt.Color/MAGENTA}))

; deploy ability generator

(defn make-deploy-ability []
  (merge
    (DeployAbility.)
    {:name "Deploy"
     :is-deploy true
     :cost 0
     :ability-colour java.awt.Color/GREEN}))

; side colours 
(def side-colours 
  {0 (.brighter (Color. 20 140 60))
   1 (.brighter (Color. 180 40 0))
   2 (.brighter (Color. 80 80 160))
   3 (.brighter (Color. 160 120 40))})

; unit type generator

(def default-unit-properties 
  {:zoc-size 1
   :ignore-zoc false
   :recharge 100
   :value 1
   :oriented-image true
   :risk-aversion 0.1
   :resource-per-turn 0
   :apsmax 0
   :capacity 0
   :size 100
   :dir 0                
   :side 0
   :source-image ic.graphics/unit-image
   :contents []
   :return-fire DEFAULT_RETURN_FIRE})

(defn make-unit-type [supplied-props] 
  (let [props (merge default-unit-properties supplied-props)
        max-power-attack (valmax (fn [a] (or (:power a) 0)) (:abilities props))]
	  (ic.engine.Unit.
	    nil 
	    (merge 
	      props
	      {:aps (:apsmax props)
	       :hps (:hpsmax props)
         :allow-enter (if (or (:allow-enter props) (> (:capacity props) 0)) true false)
	       :threat-radius (inc (or (:threat-radius props) 
	                        (max (:apsmax props) (valmax (fn [a] (or (:max-range a) 0)) (:abilities props)))))
	       :threat-level max-power-attack
         :enter-value (or (:enter-value props) (:value props))
         :zoc-size (if (= 0 max-power-attack) 0 (:zoc-size props))
	       :max-contents-size (or (:max-contents-size props) (:capacity props))}))))



(def unit-types 
  (sort-by #(:unit-type %)
  [(make-unit-type
        {:name "Steam Tank"
         :description "The venerable Steam Tank is an all-round fighting unit suited to swift attacks in open terrain. Its 6-inch cannon is effective against most units at short range."
         :value 60
         :size 8
         :unit-type UNITTYPE_MEDIUM_ARMOUR
         :move-type MOVETYPE_TANK
         :ix 0
         :iy 0
         :apsmax 5
         :hpsmax 6
         :armour 80
         :abilities [
                     (make-move-ability MOVETYPE_TANK)
                     (make-attack-ability {:name "6\" Steam Cannon"
                                           :attack-type ATTACKTYPE_LIGHT_CANNON
                                           :max-range 1
                                           :power 70
                                           :cost 1
                                           :consume-all-aps true
                                           })]})

    (make-unit-type
        {:name "Battle Tank"
         :description "With a powerful medium-range cannon and reinforced armour plating, this Battle Tank is the perfect weapon for fast moving, offensive mechanized warfare."
         :value 100
         :size 10
         :unit-type UNITTYPE_MEDIUM_ARMOUR
         :move-type MOVETYPE_TANK
         :ix 0
         :iy 12
         :apsmax 5
         :hpsmax 6
         :armour 100
         :abilities [
                     (make-move-ability MOVETYPE_TANK)
                     (make-attack-ability {:name "10\" Steam Cannon"
                                           :attack-type ATTACKTYPE_HEAVY_CANNON
                                           :max-range 2
                                           :power 80
                                           :cost 2
                                           :consume-all-aps true
                                           })]})   

    (make-unit-type
        {:name "Heavy Tank"
         :description "With two monstrous heavy cannons and massive armour, the Heavy Tank is designed to both deliver and survive a serious pounding. Use for front-line assaults in intense battles."
         :value 130
         :size 15
         :unit-type UNITTYPE_HEAVY_ARMOUR
         :move-type MOVETYPE_TANK
         :ix 0
         :iy 13
         :apsmax 4
         :hpsmax 6
         :armour 120
         :abilities [
                     (make-move-ability MOVETYPE_TANK)
                     (make-attack-ability {:name "12\" Twin Steam Cannon"
                                           :attack-type ATTACKTYPE_HEAVY_CANNON
                                           :max-range 2
                                           :power 100
                                           :cost 2
                                           :consume-all-aps true
                                           })]})   


    (make-unit-type
        {:name "Artillery Tank"
         :description "Providing mechanised armies with valuable ranged attack capability, the Artillery Tank is a versatile unit equally suited to assaulting enemy positions and setting up strong defensive lines."
         :value 80
         :size 8
         :unit-type UNITTYPE_MEDIUM_ARMOUR
         :move-type MOVETYPE_TANK
         :ix 0
         :iy 10
         :apsmax 4
         :hpsmax 5
         :armour 70
         :abilities [
                     (make-move-ability MOVETYPE_TANK)
                     (make-attack-ability {:name "12\" Medium Artillery"
                                           :attack-type ATTACKTYPE_ARTILLERY
                                           :max-range 4
                                           :min-range 2
                                           :power 80
                                           :cost 4
                                           :consume-all-aps true
                                           })]})   

    (make-unit-type
        {:name "Minelayer Tank"
         :description "Minelayer tanks are designed for the placement and removal of mines on the battlefield. They are well armoured and can rely on a light gatling gubn for defence."
         :value 120
         :size 8
         :unit-type UNITTYPE_MEDIUM_ARMOUR
         :move-type MOVETYPE_TANK
         :ix 0
         :iy 20
         :apsmax 4
         :hpsmax 6
         :armour 100
         :abilities [
                     (make-build-ability {:name "Lay Mine"    
                           :build-list (list "Mine")
                           :auto-choose-build true
                           :cost 1
                           :consume-all-aps false
                       })
                     (make-move-ability MOVETYPE_TANK)
                     (make-attack-ability {:name "0.606 Gatling Gun"
                                           :attack-type ATTACKTYPE_SMALL
                                           :max-range 1
                                           :min-range 1
                                           :power 25
                                           :cost 2
                                           :consume-all-aps false
                                           })
]})   
    
    
    (make-unit-type
        {:name "Artillery"
         :description "Artillery pieces can inflict serious damage at range against most enemy usnits. However they are slow moving and vulnerable to attack. Keep them well defended by tougher units."
         :value 80
         :size 8
         :unit-type UNITTYPE_LIGHT_VEHICLE
         :move-type MOVETYPE_TANK
         :ix 0
         :iy 14
         :apsmax 1
         :hpsmax 4
         :armour 60
         :abilities [
                     (make-move-ability MOVETYPE_TANK)
                     (make-attack-ability {:name "14\" Medium Artillery"
                                           :attack-type ATTACKTYPE_ARTILLERY
                                           :max-range 5
                                           :min-range 2
                                           :power 90
                                           :cost 1
                                           :consume-all-aps true
                                           })]})   
    
    
  (make-unit-type
        {:name "Paddle Cruiser"
         :description "Designed to dominate rivers and shallow coastal areas, the Paddle Cruiser is effective for both naval warface and providing firepower to support land actions." 
         :value 220
         :size 100
         :unit-type UNITTYPE_SHIP
         :move-type MOVETYPE_SHIP
         :ix 0
         :iy 3
         :apsmax 4
         :hpsmax 6
         :armour 120
         :ignore-zoc true
         :abilities [
                     (make-move-ability MOVETYPE_SHIP)
                     (make-attack-ability {:name "12\" Naval Cannon"
                                           :attack-type ATTACKTYPE_HEAVY_CANNON
                                           :max-range 4
                                           :power 60
                                           :cost 2
                                           :consume-all-aps false
                                           })]})   

  (make-unit-type
        {:name "Transport Barge"
         :description "This transport vessel has been armour plated and fitted with powerful steam turbines for heacy duty military transport." 
         :value 50
         :size 100
         :unit-type UNITTYPE_SHIP
         :move-type MOVETYPE_DEEP_SHIP
         :ix 0
         :iy 16
         :apsmax 4
         :hpsmax 5
         :armour 70
         :capacity 90
         :ignore-zoc true
         :risk-aversion 0.8
         :abilities [
                     (make-move-ability MOVETYPE_DEEP_SHIP)
                     (make-deploy-ability)]})   
  
    (make-unit-type
        {:name "Artillery Barge"
         :description "It was a true genius who placed a huge artillery piece on an armoured barge to create a deadly naval gun platform." 
         :value 250
         :size 100
         :unit-type UNITTYPE_SHIP
         :move-type MOVETYPE_DEEP_SHIP
         :ix 0
         :iy 17
         :apsmax 4
         :hpsmax 5
         :armour 70
         :capacity 30
         :ignore-zoc true
         :risk-aversion 0.5
         :abilities [(make-attack-ability {:name "18\" Heavy Artillery"
                                           :attack-type ATTACKTYPE_ARTILLERY
                                           :max-range 6
                                           :min-range 3
                                           :power 110
                                           :cost 4
                                           :consume-all-aps true
                                           })
                     (make-move-ability MOVETYPE_DEEP_SHIP)
                     (make-deploy-ability)]})
  
    (make-unit-type
        {:name "Patrol Boat"
         :description "A swift, well-armed patrol boat suited to rapid response on the high seas. Carries powerful anti-ship torpedos for naval battles." 
         :value 70
         :size 40
         :unit-type UNITTYPE_SHIP
         :move-type MOVETYPE_SHIP
         :ix 0
         :iy 5
         :apsmax 6
         :hpsmax 5
         :armour 70
         :ignore-zoc true
         :abilities [
                     (make-move-ability MOVETYPE_SHIP)
                     (make-attack-ability {:name "6\" Light Cannon"
                                           :attack-type ATTACKTYPE_LIGHT_CANNON
                                           :max-range 1
                                           :power 60
                                           :cost 2
                                           :consume-all-aps true
                                           })
                     (make-attack-ability {:name "Torpedo"
                                           :attack-type ATTACKTYPE_TORPEDO
                                           :max-range 2
                                           :power 70
                                           :cost 2
                                           :consume-all-aps true
                                           })]})   

    (make-unit-type
        {:name "Assault Zeppelin"
         :description "Heavily armoured and heavily armed, the assault zeppelin is designed for close support of land forces in major assualts. Use it to fly over enemy lines and eliminate key threats." 
         :value 250
         :size 25
         :unit-type UNITTYPE_AIRCRAFT
         :move-type MOVETYPE_FLY
         :ix 0
         :iy 6
         :apsmax 5
         :hpsmax 6
         :armour 80
         :ignore-zoc true
         :abilities [
                     (make-move-ability MOVETYPE_FLY)
                     (make-attack-ability {:name "200lb Bomb"
                                           :attack-type ATTACKTYPE_EXPLOSIVE
                                           :max-range 1
                                           :power 160
                                           :cost 4
                                           :consume-all-aps true
                                           })
                     (make-attack-ability {:name "Gatling Cannon"
                                           :attack-type ATTACKTYPE_LIGHT_CANNON
                                           :max-range 2
                                           :power 40
                                           :cost 2
                                           :consume-all-aps false
                                           })]})  

    (make-unit-type
        {:name "Death Ray Balloon"
         :description "Mounted with a powerful death ray projector, this massive air balloon is designed to provide both reconnaisance and control over the skies." 
         :value 100
         :size 15
         :unit-type UNITTYPE_AIRCRAFT
         :move-type MOVETYPE_FLY
         :ix 6
         :iy 6
         :apsmax 2
         :hpsmax 4
         :armour 40
         :oriented-image false
         :ignore-zoc true
         :abilities [
                     (make-move-ability MOVETYPE_FLY)
                     (make-attack-ability {:name "Death Ray"
                                           :attack-type ATTACKTYPE_DEATH_RAY
                                           :max-range 3
                                           :power 60
                                           :cost 1
                                           :consume-all-aps true
                                           })]})  

     (make-unit-type
        {:name "Observation Balloon"
         :description "Flying high over the battlefield, this observation balloon can provide a significant strategic advantage through reconnaisance and improved direction of artillery fire." 
         :size 5
         :value 30
         :unit-type UNITTYPE_AIRCRAFT
         :move-type MOVETYPE_FLY
         :ix 7
         :iy 6
         :apsmax 3
         :hpsmax 3
         :risk-aversion 0.2
         :armour 30
         :oriented-image false
         :ignore-zoc true
         :abilities [
                     (make-move-ability MOVETYPE_FLY)]})  
    
   (make-unit-type
        {:name "Rifles"
         :value 40
         :size 2
         :description "Hardened by years of campaigning, these veteran riflemen are sturdy warriors dedicated to their cause. Effective at capturing buildings and setting up defensive positions in rough terrain."
         :unit-type UNITTYPE_INFANTRY
         :move-type MOVETYPE_INFANTRY
         :ix 0
         :iy 1
         :can-capture true
         :apsmax 3
         :hpsmax 5
         :armour 60
         :abilities [
                     (make-move-ability MOVETYPE_INFANTRY)
                     (make-attack-ability {:name "0.303 Rifle"
                                           :attack-type ATTACKTYPE_SMALL
                                           :max-range 1
                                           :power 60
                                           :cost 1
                                           :consume-all-aps true
                                           })]})

   (make-unit-type
        {:name "Militia"
         :value 20
         :size 2
         :description "An irregular militia unit. These troops are poorly trained and unlikely to put up an effective fight."
         :unit-type UNITTYPE_INFANTRY
         :move-type MOVETYPE_INFANTRY
         :ix 0
         :iy 15
         :apsmax 2
         :hpsmax 4
         :armour 50
         :abilities [
                     (make-move-ability MOVETYPE_INFANTRY)
                     (make-attack-ability {:name "Muskets"
                                           :attack-type ATTACKTYPE_SMALL
                                           :max-range 1
                                           :power 40
                                           :cost 2
                                           :consume-all-aps true
                                           })]})   

   (make-unit-type
        {:name "Tripod"
         :value 120
         :size 25
         :description "A military tripod, towering over the battlefield. These impressive weapons are feared for their combination of speed, armour and formidable death ray armament."
         :unit-type UNITTYPE_MEDIUM_ARMOUR
         :move-type MOVETYPE_INFANTRY
         :ix 0
         :iy 18
         :apsmax 4
         :hpsmax 5
         :armour 90
         :abilities [
                     (make-move-ability MOVETYPE_INFANTRY)
                     (make-attack-ability {:name "Death Ray"
                                           :attack-type ATTACKTYPE_DEATH_RAY
                                           :max-range 3
                                           :power 50
                                           :cost 2
                                           :consume-all-aps false
                                           })]})   

      (make-unit-type
        {:name "Heavy Walker"
         :value 320
         :size 50
         :description "A titan of the battlefield, this fearsome war machine belches clouds of steam as it advances on terrified foes. Armed with a rapid-firing death ray for smaller foes and heavy artillery for longer range attacks."
         :unit-type UNITTYPE_HEAVY_ARMOUR
         :move-type MOVETYPE_INFANTRY
         :ix 0
         :iy 19
         :capacity 6
         :apsmax 3
         :hpsmax 7
         :armour 120
         :abilities [
                     (make-move-ability MOVETYPE_INFANTRY)
                     (make-attack-ability {:name "Death Ray"
                                           :attack-type ATTACKTYPE_DEATH_RAY
                                           :max-range 3
                                           :power 50
                                           :cost 1
                                           :consume-all-aps false
                                           })
                     (make-attack-ability {:name "15\" Heavy Artillery"
                                           :attack-type ATTACKTYPE_ARTILLERY
                                           :max-range 5
                                           :min-range 2
                                           :power 110
                                           :cost 3
                                           :consume-all-aps true
                                           })
                     (make-deploy-ability)]})   
   
     (make-unit-type
        {:name "Fortress Turret"
         :description "Armed with a powerful and accurate medium-range cannon, Fortress Turrets are ideal for defending strategic positions. Their firepower can devastate lightly armoured attackers."
         :value 80
         :size 20
         :unit-type UNITTYPE_EMPLACEMENT
         :move-type MOVETYPE_IMMOBILE
         :threat-radius 3
         :ix 0
         :iy 2
         :apsmax 2
         :hpsmax 5
         :armour 80
         :abilities [
                     (make-attack-ability {:name "8\" Turret Cannon"
                                           :attack-type ATTACKTYPE_LIGHT_CANNON
                                           :max-range 3
                                           :power 50
                                           :cost 1
                                           :consume-all-aps false
                                           })]})

     (make-unit-type
        {:name "Pillbox"
         :description "Armed with a powerful and accurate medium-range cannon, Gun Turrets are ideal for defending strategic positions. Their firepower can devastate lightly armoured attackers."
         :value 30
         :size 10
         :unit-type UNITTYPE_EMPLACEMENT
         :move-type MOVETYPE_IMMOBILE
         :threat-radius 2
         :ix 0
         :iy 11
         :allow-enter true
         :capacity 2
         :apsmax 1
         :hpsmax 5
         :armour 80
         :abilities [
                     (make-attack-ability {:name "5\" Turret Cannon"
                                           :attack-type ATTACKTYPE_LIGHT_CANNON
                                           :max-range 2
                                           :power 50
                                           :cost 1
                                           :consume-all-aps true
                                           })
                     (make-deploy-ability)]})

     (make-unit-type
        {:name "Bunker"
         :description "Protected multiple levels of concrete and stone, your forces will be safe inside this fortified military Bunker. Bunkers cannot be captured by enemy troops."
         :value 200
         :size 120
         :unit-type UNITTYPE_EMPLACEMENT
         :move-type MOVETYPE_IMMOBILE
         :threat-radius 0
         :ix 6
         :iy 11
         :oriented-image false
         :allow-enter true
         :capacity 100
         :apsmax 0
         :hpsmax 8
         :armour 200
         :abilities [(make-deploy-ability)]})

     (make-unit-type
        {:name "Mine"
         :description "Landmines are highly effective at preventing enemy units from advancing through areas that you control. Friendly units can pass safely thanks to advanced remote signalling that can temporarily disable the exoplosives."
         :value 10
         :enter-value -10
         :size 1
         :unit-type UNITTYPE_EMPLACEMENT
         :move-type MOVETYPE_IMMOBILE
         :threat-radius 0
         :ix 6
         :iy 4
         :oriented-image false
         :hpsmax 4
         :armour 100
         :abilities []})

     (make-unit-type
        {:name "Target Globe"
         :description "A target for military training."
         :unit-type UNITTYPE_EMPLACEMENT
         :move-type MOVETYPE_IMMOBILE
         :threat-radius 0
         :ix 7
         :iy 5
         :oriented-image false
         :hpsmax 1
         :armour 30
         :abilities []})
     
     (make-unit-type
        {:name "Target Boiler"
         :description "A rusty old boiler. Not much use for anything except target practice."
         :unit-type UNITTYPE_EMPLACEMENT
         :move-type MOVETYPE_IMMOBILE
         :threat-radius 0
         :ix 7
         :iy 4
         :oriented-image false
         :hpsmax 6
         :armour 80
         :abilities []})
     
    (make-unit-type
        {:name "Armoured Train"
         :description "The armoured train is designed for rapid deployment and defence of vital rail infrastructure. Use it to transport troops and clear the tracks of troublesome opposition."
         :value 200
         :size 30
         :unit-type UNITTYPE_HEAVY_ARMOUR
         :move-type MOVETYPE_RAIL
         :ix 0
         :iy 7
         :allow-enter true
         :capacity 5
         :apsmax 5
         :hpsmax 8
         :armour 100
         :abilities [
                     (make-move-ability MOVETYPE_RAIL)
                     (make-attack-ability {:name "10\" Assault Cannon"
                                           :attack-type ATTACKTYPE_LIGHT_CANNON
                                           :max-range 2
                                           :power 60
                                           :cost 2
                                           :consume-all-aps true
                                           })
                     (make-deploy-ability)]})     

    (make-unit-type
        {:name "Artillery Train"
         :description "With impressive range and devastating power, rail artillery is capable of dominating the battlefield for miles around."
         :value 250
         :size 30
         :unit-type UNITTYPE_MEDIUM_ARMOUR
         :move-type MOVETYPE_RAIL
         :threat-radius 7
         :ix 0
         :iy 9
         :apsmax 4
         :risk-aversion 0.6
         :hpsmax 4
         :armour 50
         :abilities [
                     (make-move-ability MOVETYPE_RAIL)
                     (make-attack-ability {:name "20\" Heavy Artillery"
                                           :attack-type ATTACKTYPE_ARTILLERY
                                           :max-range 7
                                           :min-range 3
                                           :power 140
                                           :cost 4
                                           :consume-all-aps true
                                           })]})     
    
    
    (make-unit-type
        {:name "Transport Train"
         :description "The transport train is capable of rapidly moving troops and mechanised units over long distances. It is however vulnerable to attack - caution should be exercised in hostile areas."
         :value 60
         :size 40
         :unit-type UNITTYPE_MEDIUM_ARMOUR
         :move-type MOVETYPE_RAIL
         :ix 0
         :iy 8
         :allow-enter true
         :capacity 25
         :apsmax 5
         :hpsmax 4
         :armour 50
         :risk-aversion 0.9
         :abilities [
                     (make-move-ability MOVETYPE_RAIL)
                     (make-deploy-ability)]})     

    
    (make-unit-type
        {:name "Construction Crawler"
         :description "These slow-moving, tracked giants belch out fumes as they do their work of constructing buildings, fortifications and key military infrastructure."
         :value 200
         :size 30
         :unit-type UNITTYPE_MEDIUM_ARMOUR
         :move-type MOVETYPE_TANK
         :ix 0
         :iy 4
         :risk-aversion 0.8
         :apsmax 3
         :hpsmax 5
         :armour 80
         :abilities [
                     (make-build-ability {:name "Construct"    
                                          :build-list (list "Factory" "Fortress Turret" "Pillbox" "Bunker")
	                                        :cost 3
	                                        :consume-all-aps true
	                                         })
                     (make-move-ability MOVETYPE_TANK)]})     

     (make-unit-type
        {:name "Village"
         :description "A small peasant village. Useful for providing resources for your army."
         :value 100
         :capacity 25
         :size 50
         :unit-type UNITTYPE_BUILDING
         :move-type MOVETYPE_IMMOBILE
         :ix 6
         :iy 2
         :apsmax 0
         :allow-enter true
         :allow-capture true
         :oriented-image false
         :resource-per-turn 25
         :hpsmax 6
         :armour 200
         :abilities [(make-deploy-ability)]}) 
     
      (make-unit-type
        {:name "Factory"
         :description "A military factory designed for rapid production of fighting units."
         :value 300
         :size 100
         :capacity 50
         :unit-type UNITTYPE_BUILDING
         :move-type MOVETYPE_IMMOBILE
         :ix 7
         :iy 2
         :apsmax 1
         :allow-enter true
         :allow-capture true
         :oriented-image false
         :hpsmax 6
         :armour 200
         :abilities [
                      (make-build-ability {:name "Construct"    
                           :build-list (list "Steam Tank" "Battle Tank" "Heavy Tank" "Artillery Tank" "Minelayer Tank" "Construction Crawler" "Patrol Boat" "Paddle Cruiser" "Assault Zeppelin" "Death Ray Balloon" "Observation Balloon" "Rifles" "Militia" "Armoured Train" "Artillery Train" "Transport Train")
                           :cost 1
                           :consume-all-aps true
                       })
                      (make-deploy-ability)]}) 

     
    ]))

(defn find-ability [unit ability-name]
  (let [abilities (:abilities unit)]
    (reduce
      (fn [found ability]
        (if found
          found
          (if 
            (= ability-name (:name ability))
            ability
            nil)))
      nil
      abilities)))

(def unit-type-map 
   (reduce 
     (fn [m t] (assoc m (:name t) t)) 
     {} 
     unit-types))


(defn unit 
  (^ic.engine.Unit [name] 
	  (if-let [type (unit-type-map name)]
		  type
	    (throw (Error. (str "Unit type not found: " name)))))
  (^ic.engine.Unit [name props]
    (merge (unit name) props)))

(defn random-unit 
  (^ic.engine.Unit []
    (let [name (rand-choice (keys unit-type-map))] 
      (unit name))))
