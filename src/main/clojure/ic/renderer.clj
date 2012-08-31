(ns ic.renderer
  (:use [ic protocols engine])
  (:use [mc.util])
  (:require [mc.resource])
  (:require [mc.ui])
  (:require [ic.game])
  (:require [ic.command])
  (:require [ic.map])
  (:require [ic.sounds])
  (:require [ic.units])
  (:import [java.awt Rectangle Dimension Graphics Color])
  (:import (mikera.util Rand))
  (:import (mikera.engine Hex)))

; Constants
(def ^:const RATIO (double (/ (Math/sqrt 3) 2))) ; x ratio for hex tile offset
(def ^:const TILESIZE 128) ; size of each tile
(def ^:const HALF_TILESIZE (long (* TILESIZE 0.5))) ; half size of each tile
(def ^:const X_OFFSET (long (* TILESIZE RATIO 0.5)))
(def ^:const Y_OFFSET (long (* TILESIZE 0.5 0.87)))
(def ^:const Y_OFFSET_X (long (* Y_OFFSET 0.5)))

(def ^:const Y_OFFSET_HEIGHT (long 3))

(def ^:const MAP_ZOOM 1.0)

(def ^:const OPTION_DRAW_STATUS true)


; length of specific animation sequences in millis
(def ^:const MOVE_ANIMATION_MILLIS 800)
(def ^:const SHOOT_ANIMATION_MILLIS 500)
(def ^:const SMOKE_ANIMATION_MILLIS 5000)


; screen coordinate functions

(defn screenx [x y]
  (int (* x X_OFFSET)))

(defn screeny [x y]
  (int (+ (* y Y_OFFSET) (* x Y_OFFSET_X))))


; Animation state and access functions
; length of total animation loop in seconds
(def ANIM_CYCLE (float 60))

(def animations (atom {}))

; transitions slowly from 0 to 1 over an ANIM_CYCLE second period
(def anim-time 
  (atom (float 0)))

; current animation millisecond
(def anim-millis 
  (atom (long (System/currentTimeMillis))))

(defn anim-pos [imax hz]
  "Returns an integer from 0 to imax-1, cycling with frequency hz"
  (let [cycles (int (* ANIM_CYCLE hz))
        fpos (mod @anim-time (/ cycles))
        ipos (int (* fpos imax cycles))] 
    ipos))

(defn icon-anim-pos [] 
  (anim-pos 16 2.0))

(defn add-animations [anims]
	(do
    (doseq [anim anims]
      (if-let [sound (:sound anim)]
        (ic.sounds/play sound)))
	  (swap! animations 
	    (fn [old-animations] 
		    (merge
	        old-animations 
		      (zipmap (map :key anims) anims))))))

; rendering functions

(defn draw-cursor-icon [#^Graphics g x y elv i]
   (let [ tx (- (screenx x y) HALF_TILESIZE)
          ty (- (screeny x y) HALF_TILESIZE elv)
          sx (* TILESIZE (icon-anim-pos))
          sy (* TILESIZE i)
          img ic.graphics/icon-image]
     (do
      (.drawImage g img 
        tx ty
        (+ tx TILESIZE) (+ ty TILESIZE)
        sx sy
        (+ sx TILESIZE) (+ sy TILESIZE)
        nil))))

(defn draw-ability-icon [#^Graphics g x y elv ability apcost]
  (let [ tx (- (screenx x y) 15)
         ty (- (screeny x y) 16 elv)] 
    (draw-cursor-icon g x y elv (ability-icon ability))
    (.setColor g (or (:ability-colour ability) java.awt.Color/WHITE))
    (.setFont g ic.graphics/mini-font)
    (mikera.ui.Draw/drawCentredText g (str apcost) tx ty)))

(defn draw-hex-terrain [^Graphics g x y scrollx scrolly  elv ^ic.engine.Terrain t]
  (let [
        tx (- (screenx x y) scrollx (centrex t))
        ty (- (screeny x y) scrolly (centrey t) elv)
        sx (sourcex t)
        sy (sourcey t)
        sw (sourcew t)
        sh (sourceh t)
        img (source-image t)
        ]
     (do
      (.drawImage g img 
        tx ty
        (+ tx sw) (+ ty sh)
        sx sy
        (+ sx sw) (+ sy sh)
        nil))))

(def BAR_WIDTH (int (* TILESIZE 0.14)))
(def BAR_HEIGHT (int 5))
(def BAR_Y_OFFSET (int (* TILESIZE 0.16)))

(defn is-player-unit [u]
  (= 0 (:side u)))

(defn side-colour [u]
  (if 
    (is-player-unit u)
    java.awt.Color/GREEN
    java.awt.Color/RED))

(defn ap-bar-colour [u]
  (if 
    (is-player-unit u)
    ([java.awt.Color/WHITE 
      java.awt.Color/WHITE
      java.awt.Color/WHITE
      java.awt.Color/WHITE
      java.awt.Color/WHITE
      java.awt.Color/WHITE
      java.awt.Color/WHITE
      java.awt.Color/GRAY 
      java.awt.Color/BLACK 
      java.awt.Color/GRAY] 
      (anim-pos 10 3))
    java.awt.Color/GRAY))

(defn draw-unit-status [^Graphics g tx ty ^ic.engine.Unit u]
    (let [bx (+ tx (centrex u))
          by (+ ty (centrey u) BAR_Y_OFFSET)
          hps (:hps u)
          hpsmax (:hpsmax u)
          hp-fraction (/ (float hpsmax))
          aps (:aps u)
          apsmax (:apsmax u)]
      (if OPTION_DRAW_STATUS (do                                 
        (if (< hps hpsmax) (do
          (.setColor g java.awt.Color/BLACK)
          (.fillRect g bx by BAR_WIDTH BAR_HEIGHT)
	        (.setColor g (side-colour u))
	        (dotimes [i hps]
	          (let [x1 (int (+ bx (* BAR_WIDTH hp-fraction i)))
	                x2 (dec (int (+ bx (* BAR_WIDTH hp-fraction (inc i)))))]
	            (.fillRect g x1 (inc by) (- x2 x1) (- BAR_HEIGHT 2))))))
        (if (and (is-player-unit u) (> apsmax 0)) 
          (let [ap-fraction (/ (float apsmax))]
		        (.setColor g java.awt.Color/BLACK)
	          (.fillRect g (- bx BAR_WIDTH) by BAR_WIDTH BAR_HEIGHT)
		        (.setColor g (ap-bar-colour u))
		        (dotimes [i aps]
		          (let [x1 (int (- bx (* ( dec BAR_WIDTH) ap-fraction (inc i))))
		                x2 (dec (int (- bx (* (dec BAR_WIDTH) ap-fraction i))))]
		            (.fillRect g x1 (inc by) (- x2 x1) (- BAR_HEIGHT 2))))))))
      (let [contents (:contents u)
            cc (count contents)]
        (if (> cc 0)
          (do
            (.setFont g ic.graphics/mini-font)
            (.setColor g Color/YELLOW)
            (mikera.ui.Draw/drawCentredText g (str "[" cc "]") (+ tx 50) (+ ty 10)))))))

(defn draw-unit [^Graphics g x y elv ^ic.engine.Unit u]
  (let [tx (int (- (screenx x y) (centrex u)))
        ty (int (- (screeny x y) (centrey u) elv))
        sx (int (sourcex u))
        sy (int (sourcey u))
        sw (int (sourcew u))
        sh (int (sourceh u))
        bx (+ tx (centrex u))
        by (+ ty (centrey u) BAR_Y_OFFSET)

        img (source-image u)
        hps (:hps u)
        hpsmax (:hps u)
        aps (:hps u)
        apsmax (:hps u)]
    (do
      (.drawImage g img 
        tx ty
        (+ tx sw) (+ ty sh)
        sx sy
        (+ sx sw) (+ sy sh)
        nil)
      (draw-unit-status g tx ty u))))


(defn check-adjacent-terrain [game x y i pred]
  (let [t (get-terrain game (+ x (Hex/dx i)) (+ y (Hex/dy i)))]
    (if t
      (pred t)
      nil)))

(defn check-adjacent-terrain-with-opposite [game x y i pred]
  (let [t (get-terrain game (+ x (Hex/dx i)) (+ y (Hex/dy i)))]
    (if t
      (pred t)
      ; check opposite direction on nil to allow rails / roads to run off map
      (let [ot (get-terrain game (- x (Hex/dx i)) (- y (Hex/dy i)))] 
        (if ot
          (pred ot)
          nil)))))


(defn draw-hex-decorations [^Graphics g game x y scrollx scrolly  elv t]
  (let [has-road (:has-road t)
        has-rail (:has-rail t)
        TSIZE TERRAIN_SIZE
        tx (- (screenx x y) scrollx (/ TSIZE 2))
        ty (- (screeny x y) scrolly (/ TSIZE 2) Y_OFFSET_HEIGHT)
        sx (* 8 TSIZE)
        sw TSIZE
        sh TSIZE]
    (if (:is-water t) (dotimes [i 6]
      (let [sy (* TSIZE i)
            adj-land (check-adjacent-terrain game x y i #(not (:is-water %)))]
          (if adj-land
            (.drawImage g ic.graphics/terrain-image 
              tx ty
              (+ tx sw) (+ ty sh)
              (+ sx (* 5 TSIZE)) sy
              (+ sx (* 5 TSIZE) sw) (+ sy sh)
              nil)))))
    (if (or has-road has-rail) (dotimes [i 6]
      (let [sy (* TSIZE i)
            adj-road (and has-road (check-adjacent-terrain-with-opposite game x y i :has-road))
            adj-rail (and has-rail (check-adjacent-terrain-with-opposite game x y i :has-rail))]
        (if (and (> elv 0) (or adj-road adj-rail))
          (do
            (.drawImage g ic.graphics/terrain-image 
			        tx ty
			        (+ tx sw) (+ ty sh)
			        (+ sx TSIZE) sy
			        (+ sx TSIZE sw) (+ sy sh)
			        nil))))))
    (if has-road (dotimes [i 6]
      (let [sy (* TSIZE i)
            adj-road (and has-road (check-adjacent-terrain-with-opposite game x y i :has-road))]
        (if adj-road
          (do
            (.drawImage g ic.graphics/terrain-image 
              tx ty
              (+ tx sw) (+ ty sh)
              sx sy
              (+ sx sw) (+ sy sh)
              nil))))))
    (if (:has-wire t) (dotimes [i 6]
      (let [sy (* TSIZE i)
            adj-wire (check-adjacent-terrain game x y i :has-wire)]
          (if (not adj-wire)
            (.drawImage g ic.graphics/terrain-image 
              tx ty
              (+ tx sw) (+ ty sh)
              (+ sx (* 3 TSIZE)) sy
              (+ sx (* 3 TSIZE) sw) (+ sy sh)
              nil)
            (.drawImage g ic.graphics/terrain-image 
              tx ty
              (+ tx sw) (+ ty sh)
              (+ sx (* 4 TSIZE)) sy
              (+ sx (* 4 TSIZE) sw) (+ sy sh)
              nil)))))
    (if has-rail (dotimes [i 6]
      (let [sy (* TSIZE i)
            adj-rail (and has-rail (check-adjacent-terrain-with-opposite game x y i :has-rail))]
        (if adj-rail
          (do
            (.drawImage g ic.graphics/terrain-image 
              tx ty
              (+ tx sw) (+ ty sh)
              (+ sx (* 2 TSIZE)) sy
              (+ sx (* 2 TSIZE) sw) (+ sy sh)
              nil)))
        )))))

; Overall map drawing

(defn draw-hex [^Graphics g game x y scrollx scrolly]
  (let [t (mget (:terrain game) x y)
        u (mget (:units game) x y)
        elv (if t (* Y_OFFSET_HEIGHT (:elevation t)) 0)]
    (if t 
      (do
        (draw-hex-terrain g x y scrollx scrolly elv t)
        (draw-hex-decorations g game x y scrollx scrolly elv t)))))

(defn draw-hex-objects [^Graphics g game x y cs]
  (let [t (mget (:terrain game) x y)
        u (mget (:units game) x y)
        elv (if t (* Y_OFFSET_HEIGHT (:elevation t)) 0)]
    (do
      (if (and u (not (@animations (:id u))))
        (draw-unit g x y elv u))
      (draw cs g x y elv))))

(defn draw-map [^Graphics g  game xmin ymin xmax ymax scrollx scrolly]
  (let []
    (dotimes [iy (- ymax ymin)]
      (dotimes [ix (- xmax xmin)]
        (draw-hex g game (+ ix xmin) (+ iy ymin) scrollx scrolly)))))

(defn draw-units [^Graphics g game xmin ymin xmax ymax cs]
  (let []
    (dotimes [iy (- ymax ymin)]
      (dotimes [ix (- xmax xmin)]
        (draw-hex-objects g game (+ ix xmin) (+ iy ymin) cs)))))


; Animation handling


(defn create-animation-list [game update]
  "Creates the animation list (possibly nil) for a given game update"
  (let [anim (:animation update)]
    (if (nil? anim)
      nil
      (case (:animation-name anim)
        "Explosion" 
          (list (merge anim
            {:start-time @anim-millis
             :iy (or (:iy anim) 3)
             :key (str "explosion-" (mikera.util.Rand/r 100000))
             :sound "Explosion"}))
        "Move"
          (let [uid (:uid update)]
            (list (merge anim
              {:start-time @anim-millis
               :key uid
               :unit (get-unit game uid)})))
        "Shoot"
	        (let [shooting-unit (get-unit game (:sx anim) (:sy anim))
                ability (if shooting-unit 
                          (ic.units/find-ability shooting-unit (:ability-name anim))
                          nil)]
            (list (merge anim
	            {:start-time @anim-millis
               :sound (if ability (:sound ability) nil)
               :ability ability
	             :key (str "shoot-" (mikera.util.Rand/r 100000))})))  
        "Text float"
          (list (merge anim
            {:start-time @anim-millis
             :key (str "text-" (mikera.util.Rand/r 100000))}))  
        "Damage"
          (list 
            (merge anim
              {:animation-name "Text float"
               :colour java.awt.Color/RED
               :start-time @anim-millis
               :text (str "-" (:damage anim))
               :key (str "damage-text-" (mikera.util.Rand/r 100000))})
            (merge anim
              {:animation-name "Explosion"
               :start-time @anim-millis
               :iy (or (:iy anim) 3)
               :key (str "damage-explosion-" (mikera.util.Rand/r 100000))}))
        (throw (Error. (str "Cannot create animation for update: " update)))))))

(defn add-animations-for-updates [game updates]
  (let [anims (mapcat (fn [update] (create-animation-list game update)) updates)]
    (add-animations anims)))


(defn draw-animated-icon [^Graphics g x y ai apos]
   (let [ tx (- (screenx x y) HALF_TILESIZE)
          ty (- (screeny x y) HALF_TILESIZE)
          sx (* TILESIZE apos)
          sy (* TILESIZE ai)
          img ic.graphics/icon-image]
     (do
      (.drawImage g img 
        tx ty
        (+ tx TILESIZE) (+ ty TILESIZE)
        sx sy
        (+ sx TILESIZE) (+ sy TILESIZE)
        nil))))

(defn draw-animation [^Graphics g anim]
  "Draws an animation frame and returns the updated animation object"
  (let [time @anim-millis 
        animation-name (:animation-name anim)
        start-time (:start-time anim)
        ; p (println (str "do-animation times " time " - " start-time))
        elapsed (long (- time start-time))]
    (if (< elapsed 0)
      anim ; not yet ready to display
      (case animation-name
        "Explosion"
          (if (>= elapsed 640) 
            nil 
            (do 
              (draw-animated-icon g (:tx anim) (:ty anim) (:iy anim) (quot elapsed 80))
              anim))
        "Smoke"
          (if (>= elapsed SMOKE_ANIMATION_MILLIS) 
            nil 
            (do 
              (draw-animated-icon g (:tx anim) (:ty anim) (:iy anim) (quot elapsed 80))
              anim))
        "Move"
          (if (>= elapsed MOVE_ANIMATION_MILLIS) 
            nil 
            (let [moves (:animation-moves anim)
                  n (dec (count moves))
                  percent (/ (double elapsed) MOVE_ANIMATION_MILLIS )
                  hexes-moved (* n percent)
                  i (int hexes-moved)
                  fraction-of-hex (- hexes-moved i)
                  ^ic.engine.Point pos (nth moves i)
                  ^ic.engine.Point npos (nth moves (inc i))
                  dir (mikera.engine.Hex/direction (.x pos) (.y pos) (.x npos) (.y npos))
                  aunit (:unit anim)
                  unit (if (= dir (:dir aunit)) aunit (assoc aunit :dir dir))]
              (draw-unit 
                g 
                (mikera.util.Maths/lerp fraction-of-hex  (.x pos) (.x npos)) 
                (mikera.util.Maths/lerp fraction-of-hex  (.y pos) (.y npos)) 
                0 
                unit)
              anim))
        "Text float"
          (if (>= elapsed (* MOVE_ANIMATION_MILLIS 2)) 
            nil
            (let [x (:tx anim) ; hex position
                  y (:ty anim) ; hex position
                  text (:text anim)
                  colour (:colour anim)
                  tx (- (screenx x y) 0)
                  ty (- (screeny x y) 0 (int (/ elapsed 20)))]
              (.setFont g ic.graphics/main-font)
              (.setColor g colour)
              ;(.fillRect g tx ty 15 15)
              (mikera.ui.Draw/drawCentredText g text tx ty)
              anim))
        "Shoot"
          (if (>= elapsed SHOOT_ANIMATION_MILLIS) 
            nil
            (let [ability (:ability anim) ; included by create-animation-list if available
                  ptx (:tx anim) ; hex position
                  pty (:ty anim) ; hex position
                  psx (:sx anim) ; hex position
                  psy (:sy anim) ; hex position
                  sx (- (screenx psx psy) 0)
                  sy (- (screeny psx psy) 0)
                  tx (- (screenx ptx pty) 0)
                  ty (- (screeny ptx pty) 0)
                  attack-type (if ability (:attack-type ability) nil)
                  bolt-radius (if (= "Small arms" attack-type) 1 2)
                  shots (if (= "Small arms" attack-type) 4 3)
                  pos (mikera.util.Maths/frac (float (/ (* elapsed shots) SHOOT_ANIMATION_MILLIS)))]
              (case attack-type
	              "Death ray" (if (Rand/chance 0.5) (do
                  (.setColor g java.awt.Color/MAGENTA)
                  (.drawLine 
                    g 
                    sx 
                    sy 
                    tx 
                    ty)))
                "Artillery" (do ;default case
                  (.setColor g java.awt.Color/GRAY)
                  (.fillRect 
                    g 
                    (- (mikera.util.Maths/lerp pos sx tx) bolt-radius) 
                    (- (mikera.util.Maths/lerp pos sy ty) bolt-radius) 
                    (* 2 bolt-radius) 
                    (* 2 bolt-radius)))
                (do ;default case
	                (.setColor g java.awt.Color/WHITE)
		              (.fillRect 
		                g 
		                (- (mikera.util.Maths/lerp pos sx tx) bolt-radius) 
		                (- (mikera.util.Maths/lerp pos sy ty) bolt-radius) 
		                (* 2 bolt-radius) 
		                (* 2 bolt-radius))))
              anim))
        (throw (Error. (str "Unrecognised animation: " animation-name)))))))
      

(defn draw-animations [^Graphics g game]
  (let [anim-list (vals @animations)
       next-animations (reduce 
                     (fn [anims anim] 
                       (let [a (draw-animation g anim)]
                          (if (nil? a) anims (assoc anims (:key anim) anim)))) 
                     {}
                     anim-list)]
    (reset! animations next-animations)))

(defn draw-all-objects [g game xmin ymin xmax ymax command-state]
  (draw-units g game xmin ymin xmax ymax command-state)
  (draw-animations g game))

