(ns ic.game
  "Game handling logic and AI routines"
  (:use [ic protocols map engine])
  (:require [ic.units])
  (:use [ic.command])
  (:require [mc.util])
  (:import [mikera.persistent.SparseMap])
  (:import [mikera.persistent.LongMap])
  (:import [mikera.util.Rand])
  (:use [mc.util])
  (:use [clojure.test]))


(set! *warn-on-reflection* true)
(set! *unchecked-math* true)


(declare ai-evaluation)
(declare handle-command-event)
(declare handle-time-event)
(declare handle-turn-event)
(declare handle-god-command)


(def ^:const TURN_LENGTH_MILLIS 10000)
(def ^:const AI_THINK_MILLIS 800)
(def ^:const MAX_AI_TURN_MILLIS 2400)


(defn active-sides [game]
  "Returns a sorted list of sides with active units"
  (sort (reduce
    (fn [ss [uid ^ic.engine.Point pos]]
      (let [unit (get-unit game (.x pos) (.y pos))
            player-side (:side unit)]
        (if (not (list-contains? ss player-side))
          (conj ss player-side)
          ss)))
    '()
    (:unit-locations game))))

(defn get-player-for-side [g side]
  (find-first
    (fn [player]
      (= (:side player) side))
    (.values ^mikera.persistent.LongMap (:players g))))

(defn side-has-human? 
  "Returns true if at least one player on a side is human"
  ([g side]
    (boolean 
      (some (fn [[i p]] (and (= side (:side p)) (:is-human p))) (:players g)))))

;; =========================================================================
;; Update functions

 (defn get-updates [g event]
    (let [type (:command-type event)]
      (case type
        "Command" (handle-command-event g event)
        "God-Command" (handle-god-command g event)
        "Tick tock" (throw (Error. "Use (get-ai-commands..) instead"))
        "Next turn" 
          (let [updates (handle-turn-event g event)]
            ;(println updates)
            updates)
        (throw (Error. (str "Event type not recognised: " event))))))

 
(defn update [g update]
  (let [type ( :update-type update)] 
    ;(println (str "Game update: " update))
    (case type
      "Unit property"
        (assoc g
          :units
          (let [units (:units g)
                uid (:uid update)
                ^ic.engine.Point pos (.get ^mikera.persistent.LongMap (:unit-locations g) (long uid))
                x (.x pos)
                y (.y pos)
                unit (get-unit g x y)
                updated-unit (assoc unit (:property update) (:value update))]
            (mset units x y updated-unit)) )
      "Unit properties"
        (assoc g
          :units
          (let [units (:units g)
                uid (:uid update)]
            ;; check unit actually exists, might have been destroyed....
            (if-let [^ic.engine.Point pos (.get ^mikera.persistent.LongMap (:unit-locations g) (long uid))]
              (let [x (.x pos)
                    y (.y pos)
                    unit (get-unit g x y)
                    updated-unit (merge unit (:properties update))]
                (mset units x y updated-unit)))))
      "Player properties"
        (let [players ^mikera.persistent.LongMap (:players g)
              pid (long (:player-id update))
              player (.get players pid)
              props (:properties update)]
          (assoc g :players 
            (.include players pid (merge player props))))          
      "Remove unit"
        (let [units (:units g)
              uid (:uid update)
              ^ic.engine.Point pos (.get ^mikera.persistent.LongMap (:unit-locations g) (long uid))
              tx (.x pos)
              ty (.y pos)]
            (-> g
              (remove-unit tx ty))) 
      "Add unit"
        (let [unit (:unit update)
              tx (:tx update)
              ty (:ty update)]
            (-> g
              (add-unit tx ty unit))) 
      "Set terrain"
        (let [t ^ic.engine.Terrain (:terrain update)
              tx (int (:tx update))
              ty (int (:ty update))]
            (set-terrain g tx ty t)) 
      "Next turn"
        (let [next-turn-number (inc (:turn-number g))]
          (-> g
            (assoc :turn-number next-turn-number)
            ((fn [game] 
               (assoc game :players
                 (reduce
                   (fn [^mikera.persistent.LongMap np [player-id player]]
                     (.include np (long player-id) (assoc player :ai-evaluation (ai-evaluation game player))))
                   mikera.persistent.LongMap/EMPTY
                   (:players game))))))) 
      "Move unit"
        (let [uid (:uid update)
              ^ic.engine.Point pos (.get ^mikera.persistent.LongMap (:unit-locations g) (long uid))
              sx (:sx update)
              sy (:sy update)
              tx (:tx update)
              ty (:ty update)
              unit (get-unit g sx sy)]
            (-> g
              (remove-unit sx sy)
              (add-unit tx ty unit))) 
      "Failed command"
        (do 
          (println (str "Failed command: " update))
          g)
      (throw (Error. (str "Update type not recognised: " update))))))

 
(defn apply-command [g command]
  "Applies a command to a game, returns updated game"
  (let [updates (get-updates g command)]
    (reduce (fn [g u]
              (update g u))
            g 
            updates)))

;; ================================================================================
;; Command handling


(defn handle-god-command [g event]
  "Handle god commands - checks validity of command and translates into game updates."
  (let [update (:update event)]
    (case (:update-type update)
      "Add unit" 
        (let [tx (:tx update)
              ty (:ty update)
              u (get-unit g tx ty)]
          (concat
            (if (nil? u)
              '()
              (list (msg-remove-unit u tx ty)))
            (list update)))
      "Remove unit"
        (let [tx (:tx update)
              ty (:ty update)
              u (get-unit g tx ty)]
          (if (nil? u)
            '()
            (list (msg-remove-unit u tx ty))))
      "Set terrain"
        (let [tx (:tx update)
              ty (:ty update)
              terrain (:terrain update)]
           (list (msg-set-terrain tx ty terrain)))
      "Player properties"
        (list update)
      "Unit property"
        (list update)
      "Unit properties"
        (list update)
      (throw (Error. (str "God command not handled: " event))))))

(defn handle-command-event [g command]
  "Handle a command event to a single unit, returns list of updates"
  (let [{ cmd :command-type  uid :uid abname :ability  tx :tx ty :ty player-id :player-id} command
        sp (location-of-unit g uid)]
    ;(println event)
    (cond 
      (nil? sp)
        (list (msg-command-fail (str "Unit not found for command " command)))
      :default
	      (let [x (.x sp)
	            y (.y sp)
	            u (get-unit g x y)] 
	        (cond 
            (not= player-id (:player-id u))
              (list (msg-command-fail (str "Illegal player ID " player-id " for unit " u)))
            :default
		          (let [ability (ic.units/find-ability u abname)] 
		            (apply-to-game ability g u x y tx ty command)))))))

; ===================================================================================
;
; AI map evaluation - build a map of values for the current game state

(defn make-influence-map [^doubles arr ^Integer ox ^Integer oy ^Integer w ^Integer h]
  "Converts an array of doubles into a SparseMap"
  (let [m (atom (new-map))]
;    (println (str "influence-map: " w "*" h " at " ox "," oy))
    (dotimes [ix w] (dotimes [iy h]
      (let [v ^Double (aget arr (unchecked-add ix (unchecked-multiply iy w)))]
		    (reset! m (.update 
		                 ^mikera.persistent.SparseMap @m 
		                 (+ ox ix) 
		                 (+ oy iy)
                     v)))))
    @m))

(defn threat-radius ^double [unit]
  (double (:threat-radius unit)))

(defn threat-level ^double [unit]
  (double (:threat-level unit)))

(defn objective-value ^double [unit]
  (double (:value unit)))


(defn calc-threat-level [unit sx sy tx ty]
  (let [dist (mikera.engine.Hex/distance sx sy tx ty)
        radius (threat-radius unit)]
    (if (<= dist radius)
      (* (threat-level unit) (- 1 (/ dist radius 2)))
      0.0)))

(defn calc-objective-value [unit sx sy tx ty]
  (let [dist (mikera.engine.Hex/distance sx sy tx ty)
        factor (+ 1.0 (* 0.2 dist))]
    (/ (objective-value unit) (* factor factor factor))))

(defn ai-evaluation 
  "Evaluates the current position from the perspective of a player"
  [game player]
  (let [terrain ^mikera.persistent.SparseMap (get-map game)
        units (:units game)
        bounds ^mikera.math.Bounds4i (.getNonNullBounds terrain)
        ox (.xmin bounds)
        oy (.ymin bounds)
        w (unchecked-subtract (inc (.xmax bounds)) ox)
        h (unchecked-subtract (inc (.ymax bounds)) oy)
        asize (unchecked-multiply w h)
        enemy-threat-level ^doubles (make-array Double/TYPE asize)
        friendly-threat-level ^doubles (make-array Double/TYPE asize)
        objective-value ^doubles (make-array Double/TYPE asize)
        side (:side player)]
	  (do
	    (dotimes [ix w] (dotimes [iy h]
        (let [x (unchecked-add ix ox)
              y (unchecked-add iy oy)
              ind (unchecked-add ix (unchecked-multiply iy w))]
          (mvisit units
            (fn [ux uy unit]
              (if (= side (:side unit))
                (aset friendly-threat-level ind (+ (double (aget friendly-threat-level ind)) (double (calc-threat-level unit ux uy x y))))
                (do
                  (aset enemy-threat-level ind (+ (double (aget enemy-threat-level ind)) (double (calc-threat-level unit ux uy x y))))
                  (aset objective-value ind (+ (double (aget objective-value ind)) (double (calc-objective-value unit ux uy x y)))))))))))
     
      {:enemy-threat-level (make-influence-map enemy-threat-level ox oy w h)
		   :friendly-threat-level (make-influence-map friendly-threat-level ox oy w h)
		   :objective-value (make-influence-map objective-value ox oy w h)}
      )))


; AI logic

(defn ai-ready-to-move [millis base]
  (< (long (/ base AI_THINK_MILLIS)) (long (/ (+ base millis) AI_THINK_MILLIS))))



(defn get-ai-action
  "Gets the best AI command for a given unit, or nil if none"
  ([game unit x y]
    (let [command (ic.units/ai-best-command game unit x y)]
      ;(println (str "Command: " command))
      (if (nil? command)
        nil
        (list (assoc command :player-id (:player-id unit)))))))

(defn is-controlled? 
  "Returns true if the unit is controlled by a specific player"
  ([unit player]
    (= (:id player) (:player-id unit))))

(defn is-ai-controlled-unit? 
  "Returns true if specific unit is controlled by an AI"
  ([game unit]
    (let [player (get-player game (:player-id unit))] 
      (boolean (:ai-controlled player)))))

; time update handling

(defn get-ai-command [g player u x y]
  "Creates ai action command list (or nil) for a single unit"
  (if 
    (or (> (:aps u) 0) (not (empty? (:contents u))))
    (get-ai-action g u x y)
    nil))

(defn get-ai-commands 
  "Gets a list of all ai-commands for the given game"
  ([g ]
    (get-ai-commands g (fn [u] true)))

  ([g unit-pred]
    (let [^mikera.persistent.LongMap unit-locs (:unit-locations g)
          ^mikera.persistent.SparseMap units (:units g)]
      ;(println event)
      (mapcat
        (fn [^ic.engine.Point pos]
          (let [x (.x pos)
                y (.y pos)
                u (mget units x y)
                player (.get ^mikera.persistent.LongMap (:players g) (long (:player-id u)))] 
            (if 
              (and 
                (:ai-controlled player)
                (unit-pred u))
              (get-ai-command g player u x y))))
        (seq (.values unit-locs))))))

;; ===========================================
;; next-turn handling

(defn handle-unit-turn [g u x y millis base]
  "Handles the end of turn event for a unit, returning a list of updates"
  (let [apsmax (:apsmax u)
        ap-recharge-period (if (> apsmax 0) (/ (* 100 TURN_LENGTH_MILLIS) apsmax (:recharge u)) 1000000)
        ap-bonus (mikera.util.Maths/quantize millis ap-recharge-period base)
        aps (:aps u)
        contents (:contents u)
        new-aps (int (min apsmax (+ ap-bonus (max 0 aps))))]
    (list-not-nil
	    (if 
	      (not (= aps new-aps))
	      (msg-update-unit u :aps new-aps)
	      nil)
      (if
        (not (empty? contents))
        (msg-update-unit u :contents 
          (map-vector (fn [unit] (assoc unit :aps (:apsmax unit))) contents))
        nil))))


(defn handle-turn-event [g event]
  "Handles the End of Turn event, returning list of updates"
  (let [{ cmd :command-type } event
        ^mikera.persistent.LongMap unit-locs (:unit-locations g)
        resource-tally (atom {})]
    ;(println event)
    ; TODO: fix base time update
    (concat
	    (doall (mapcat
	      (fn [^ic.engine.Point pos]
	        (let [x (.x pos)
	              y (.y pos)
	              u (get-unit g x y) 
               resource-impact (:resource-per-turn u)
               player-id (:player-id u)]
            (if resource-impact 
              (do
;	              (println resource-impact)
                (swap! resource-tally 
	                (fn [old-tally]
		                (assoc old-tally player-id 
		                  (+ (or (old-tally player-id) 0) resource-impact)))))) 
	          (handle-unit-turn g u x y TURN_LENGTH_MILLIS 0)))
        (seq (.values unit-locs))))
      (doall (map 
        (fn [[player-id impact]]
          ;(println (str player-id " resources + : " impact))
          (let [player (get-player g player-id)
                new-total (max 0 (+ impact (:resources player)))]
;            (println "Add resources for " (:name player) ": " impact " -> " new-total " total")
            (msg-update-player player 
              {:resources new-total})))
        @resource-tally))
      (list (msg-next-turn)))))




