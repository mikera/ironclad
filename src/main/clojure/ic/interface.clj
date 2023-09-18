(ns ic.interface 
  "The main Ironclad user interface"
  (:require [ic.protocols :refer [Location get-x ]])
  (:require [mc.util :as mcu])
  (:require [mc.resource])
  (:require [mc.ui])
  (:require [ic.ui])
  (:require [ic engine game])
  (:require [ic.command])
  (:require [ic.dialogs])
  (:require [ic.gamefactory])
  (:require [ic.map])
  (:require [ic.renderer])
  (:require [ic.serial])
  (:require [ic.units])
  (:import (javax.swing JScrollPane JCheckBox JOptionPane JFileChooser JTextPane ListModel SwingUtilities DefaultListModel JFrame JSeparator JComponent JList JTextField JTextArea JButton JPanel BoxLayout BorderFactory Timer)
           (java.awt.event ActionListener ActionEvent MouseAdapter MouseMotionAdapter MouseEvent)
           (java.awt.image BufferedImage)
           (java.net URL)
           (java.io File)
           (javax.swing.event ListSelectionListener ListSelectionEvent)
           (java.awt Point GraphicsConfiguration Graphics2D GridLayout BorderLayout FlowLayout Rectangle Dimension Graphics Color)
           (mikera.engine Hex)
           (mikera.util Tools)
           (mikera.ui.steampunk PanelBorder)
           (ic ListCellData)
           (net.miginfocom.swing MigLayout)
           (mikera.gui BufferedImageIcon)))

(def DEBUG-PRINT-COMMANDS false)


(declare update-ui-state!)
(declare set-command-state!)
(declare nothing-selected-state)
(declare selected-unit-state)
(declare button-next-turn)
(declare view-unit-state)
(declare update-gamescreen-bounds)
(declare load-game)
(declare 
  mini-map-navigate
  scroll-to
  game-scrollpane)

;============================================================================================
; State of user interface


(def state 
  (atom 
    {:game nil
     :scroll [0 0]
     :mouseover [0 0]
     :command-state nil
     :commands (clojure.lang.PersistentQueue/EMPTY)
     :playing false
     :moving-side 0
     :ai-move false
     :player-id nil}))


(declare ^JPanel unit-status-panel)
(declare ^JPanel unit-description-panel)
(declare ^JComponent mini-map)

(def exit-function (atom (fn [] (throw (Error. "No exit function defined!!!")))))

; =========================================================================================
; GUI resources

; Screen coordinate functions

(defn logical-positionx ^double [sx sy]
  (* ic.renderer/RATIO (/ sx ic.renderer/X_OFFSET) ))

(defn logical-positiony ^double [sx sy]
  (/ sy ic.renderer/Y_OFFSET) )

(defn mapx ^long [sx sy]
  (Hex/toLocationX (logical-positionx sx sy) (logical-positiony sx sy)))

(defn mapy ^long [sx sy]
  (Hex/toLocationY (logical-positionx sx sy) (logical-positiony sx sy)))

(def ^DefaultListModel ability-list-model  
  (DefaultListModel.))
;    (.addElement (ic.ListCellData. (drawable-icon (ic.units/unit "Steam Tank")) "Some Text 2" nil))))

(def ^JList ability-list   
  (doto (JList.) 
    (.setModel ability-list-model)
    (.setCellRenderer (ic.ListCellRenderer.))
    (.setBorder (mikera.ui.steampunk.SteamPunkBorder.))
    (.addListSelectionListener 
      (proxy [ListSelectionListener] []
        (valueChanged [^ListSelectionEvent e]
          ; update command state without triggering apply-to-gui
          ;(swap! state assoc :command-state
          (set-command-state!
            (list-click 
	            (:command-state @state)
	            (let [^ListCellData lcd (.getSelectedValue ^JList ability-list)]
                (if (nil? lcd) nil (.value lcd))))))))))

(defn victory [] 
  (do
    (swap! state assoc :playing false)
    (ic.sounds/play "Fanfare")
    (if (= 
          (JOptionPane/YES_OPTION) 
          (JOptionPane/showConfirmDialog ic.frame/frame "Congratulations on your splendid victory!\n\n Would you like to return to base?" "Victory!" (JOptionPane/YES_NO_OPTION)))
      (@exit-function))))

(defn loss [] 
  (do
    (swap! state assoc :playing false)
    (ic.sounds/play "Explosion")
    (if (= 
          (JOptionPane/YES_OPTION) 
          (JOptionPane/showConfirmDialog ic.frame/frame "What rotten luck! We appear to have been beaten.\n\n Shall we return to base for a stiff drink?" "Defeat!" (JOptionPane/YES_NO_OPTION)))
      (@exit-function))))


; =============================================================================================
; Core game engine / command handler

(defn handle-command [game command]
  "Handles a given server command and returns the updated overall game"
  (if DEBUG-PRINT-COMMANDS (println (str "Command received: " command)))
  (let [updates (get-updates game command)
			  updated-game (reduce
			       (fn [g u]
                (try
			            (update g u)
							    (catch Throwable t 	                    
							      (.printStackTrace t)
                    g)))
			       game
			       updates)]    
   (ic.renderer/add-animations-for-updates game updates)
   updated-game))

(defn command-state-update [cs game]
  "Gets an updated command state if needed to allow for an updated game state"
  (try
    (update-command-state cs game)
    (catch Throwable t
      (.printStackTrace t)
      (nothing-selected-state))))

(defn get-next-side [game i]
  (let [sides (ic.game/active-sides game)]
    (or (get-element-after sides i ) (first sides))))



(def last-engine-time (atom (System/currentTimeMillis)))
(def last-turn-time (atom @last-engine-time))

(defn ^java.lang.Runnable engine-loop []
  "Engine loop handles commands"
  (do 
    ;(println "Tick!")
    (let [commands (:commands @state)
          s (swap! state assoc :commands nil)
          player-id (:player-id s)
          side (:moving-side s)
          new-time (System/currentTimeMillis)
          elapsed (- new-time @last-engine-time)
          fast-ai (:fast-ai s)
          turn-time (- new-time @last-turn-time)
          time-event (ic.command/time-event elapsed @last-engine-time)
          includes-next-turn (some (fn [c] (= "Next turn" (:command-type c))) commands)
          game (:game s)
          ai-cmds (ic.game/get-ai-commands 
	                   game 
	                   (fn [u] (and
	                     (= side (:side u))
                       (ic.game/is-ai-controlled-unit? game u)
	                     (or  ; fast ai moves as quick as possible
	                        fast-ai
	                        (ic.game/ai-ready-to-move elapsed (+ @last-engine-time (:id u)))))))
          all-cmds (concat 
                 ai-cmds
                 commands) 
          new-game (reduce 
                    (fn [game cmd] (handle-command game cmd))
                    game
                    all-cmds)
          new-side (if 
                     (if (ic.game/side-has-human? new-game side)
                        includes-next-turn 
	                       (and 
	                         (or fast-ai (> turn-time ic.game/MAX_AI_TURN_MILLIS))
	                         (= 0 (count ai-cmds))))
                     (get-next-side new-game side)
                     side)]
      (if (= side new-side) 
        nil 
        (do
          (println "Turn finished for side " side ", next side = " new-side)
          (reset! last-turn-time new-time)))
      (swap! state merge {:game new-game :moving-side new-side})
      (reset! last-engine-time new-time))
    (Thread/sleep 40)
    (recur)))

(defn ^java.lang.Runnable game-loop [] 
  "Game loop handles key interface interactions"
  (do 
    (if (:playing @state) (do
      (if (= [0] (ic.game/active-sides (:game @state)))
        (victory))
      (if (not (list-contains? (ic.game/active-sides (:game @state)) 0))
        (loss))))
    (update-ui-state!)
    (Thread/sleep 40)
    (recur)))

; Thread for driving main game engine
(def ^Thread engine-thread 
  (Thread. engine-loop))

; Thread for running commands and game screen interactions
(def ^Thread game-thread 
  (Thread. game-loop))


; Sends a unit command to the server, tagged with appropriate player ID
(defn send-command 
  ([u ability tx ty param]
		(let [cmd (ic.command/command 
	             (:id u) 
	             (:name ability) 
	             tx 
	             ty
               param)]
      (send-command cmd)))
  ([cmd]
    (let [tagged-cmd (merge cmd 
                       {:player-id (:player-id @state)})] 
	    (swap! state 
		    (fn [oldstate] 
		      (assoc oldstate :commands (conj (:commands oldstate) tagged-cmd)))))))

(defn advance-turn []
  (send-command (ic.command/next-turn)))

; ========================================================================================
; GUI updates for command state

(defn ^String unit-description-text [u]
  (str
    (str (or (:name u) "Click on a unit to see its description"))
    "\n\n"
    (str (:description u))))

(defn ^String unit-status-text [u]
  (if 
    u
	  (str
	    (str "  Type: " (:unit-type u) "\n")
	    (str "  Action points: " (:aps u) "/" (:apsmax u) "\n")
	    (str "  Hit points: " (:hps u) "/" (:hpsmax u) "\n")
	    (str "  Defence: " (:armour u) "\n")
	    (str "\n"))
    nil))

(def previously-selected-unit (atom nil))

(defn update-ui-for-selected-unit [u sus]
	(if (= u @previously-selected-unit)
    nil
    (do 
      (swap! previously-selected-unit (fn [old-unit] u))
      (doto unit-description-panel
         (.setText (unit-description-text u)))
	    (doto unit-status-panel
		     (.setText (unit-status-text u)))
		  (doto ability-list-model
		    (.clear)
		    ((fn [^ListModel lm] 
		      (if (not (nil? u)) 
	          (do
				      (doseq [ab (:abilities u)]
				        (.addElement lm 
				          (ic.ListCellData. 
			              (let [sx (+ 32)
			                    sy (+ 8 32 (* ic.renderer/TILESIZE (ability-icon ab)))
			                    sw (- ic.renderer/TILESIZE 64)
			                    sh (- ic.renderer/TILESIZE 80)]
			                (BufferedImageIcon.
			                  ic.graphics/icon-image
			                  sx sy sw sh 40 32))	            
			              (nameString ab u) 
				            ab)))
	            (.validate ability-list))
            nil)))))))

; target map functions


(defn target-map 
  "Gets a SparseMap of (x,y)-> ability"
  ([targets]
    (target-map targets (fn [ability] true)))
  ([targets ability-filter]
	  (reduce 
	    (fn [sm [ability ability-target-map]]
		    (if (ability-filter ability)
          (reduce
		        (fn [ssm [^Location p v]] 
		          (mset ssm (get-x p) (get-y p) ability))
			      sm
		        ability-target-map)
          sm))
		  (new-map)
	    targets)))

(defn apcost-map [targets target-map]
  "Gets a SparseMap of (x,y)-> rounded up ap cost"
  (reduce 
    (fn [sm [ability ability-target-map]]
      (reduce
        (fn [ssm [^Location p ^Double v]] 
          (let [x (get-x p)
                y (get-y p)]
            (if (= ability (mget target-map x y))
              (mset ssm x y (mikera.util.Maths/roundUp v))
              ssm)))
        sm
        ability-target-map))
    (new-map)
    targets))

(defn get-ability-param [game unit ability tx ty]
  (cond
    (and  (:build-list ability) (not (:auto-choose-build ability)))
      (ic.dialogs/show-select-unit-dialog 
        "Select a unit to build: " 
        (ic.units/buildable-unit-names ability game unit tx ty))
    (:is-deploy ability)
      (let [contents (:contents unit)
            deploy-unit
				      (ic.dialogs/show-select-unit-dialog 
				        "Select a unit to delpoy: " 
				        (filter #(ic.units/suitable-terrain? % (get-terrain game tx ty)) contents))
            pos (find-position contents deploy-unit)]
        pos)
    :default nil))

; state for issuing a command to a selected unit
(defrecord SelectedUnitState []
  PCommandState
    (left-click [c x y]
      (let [u (:unit c)
            game (:game @state)
            tu (get-unit game x y)
            targets (:targets c) 
            tability (mget targets x y)
            param (get-ability-param game u tability x y)]
        ;(println "left click!")
        (if tability
	        (if (or (not (needs-param? tability)) param)
	          (do 
	            (send-command u tability x y param)
	            c)
            c)
          (if (nil? tu) 
            (nothing-selected-state)          
            (selected-unit-state tu x y nil)))))
    (right-click [c x y]
      (let [u (get-unit (:game @state) x y)]
        (if (nil? u)
          (nothing-selected-state)
          (selected-unit-state u x y nil))))
    (list-click [c v]
      (let [ux (:x c)
            uy (:y c)
            u (:unit c)]
        (if (= v (:ability c))
          c
          (selected-unit-state u ux uy v))))
    (mouse-dragged [c x y left-button-down right-button-down]
      (if left-button-down (left-click c x y) (right-click c x y))) 
    (update-command-state [c g]
      (let [uid (:id (:unit c))
            unit (if (nil? uid) nil (get-unit g uid))]
        (if 
          (not (nil? unit))
          (let [upos (location-of-unit g unit)
                ux (.x upos)
                uy (.y upos)] 
            (selected-unit-state unit ux uy (:ability c)))
          (nothing-selected-state))))
    (apply-to-gui [c]
      (update-ui-for-selected-unit (:unit c) c))
    (draw [c g x y elv]
      (let [ux (:x c)
            uy (:y c)
            targets (:targets c) 
            tability (mget targets x y)]
        (do
	        (if (and (= x ux) (= y uy))
	          (ic.renderer/draw-cursor-icon g x y elv 0))
          (if (not (nil? tability))
            (ic.renderer/draw-ability-icon g x y elv tability (mget (:costs c) x y))))))  ; (ic.units/ability-icon tability)
    (get-command [c]
      nil))

; state for a unit being inspected
(defrecord ViewUnitState []
  PCommandState
    (left-click [c x y]
      (let [u (get-unit (:game @state) x y)]
        (if (nil? u)
          (nothing-selected-state)
          (selected-unit-state u x y nil))))
    (right-click [c x y]
      (let [u (get-unit (:game @state) x y)]
        (if (nil? u)
          (nothing-selected-state)
          (view-unit-state u x y nil))))
    (list-click [c v]
      (let [ux (:x c)
            uy (:y c)
            unit (:unit c)]
        (selected-unit-state unit ux uy v)))
    (draw [c g x y elv]
      (let [ux (:x c)
            uy (:y c)]
        (do
          (if (and (= x ux) (= y uy))
            (ic.renderer/draw-cursor-icon g x y elv 0)))))  
    (update-command-state [c g]
      (let [ux (:x c)
            uy (:y c)
            unit (get-unit g ux uy)
            uid (:id (:unit c))]
        (if 
          (and 
            (not (nil? unit))
            (= (:id unit) uid))
          (view-unit-state unit ux uy)
          (nothing-selected-state))))
    (mouse-dragged [c x y left-button-down right-button-down]
      (if left-button-down (left-click c x y) (right-click c x y)))
    (apply-to-gui [c]
      (update-ui-for-selected-unit (:unit c) c))
    (get-command [c]
      nil))

; state for map painting
(defrecord PainterState []
  PCommandState
    (left-click [c x y]
      (let [cmd-function @ic.dialogs/paint-left-click
            game (:game @state)
            cmd (cmd-function game x y)]
        (if cmd (send-command (ic.command/god-command cmd)))
        c))
    (right-click [c x y]
      (let [cmd-function @ic.dialogs/paint-right-click
            game (:game @state)
            cmd (cmd-function game x y)]
        (if cmd (send-command (ic.command/god-command cmd)))
        c))
    (list-click [c v]
      c)
    (mouse-dragged [c x y left-button-down right-button-down]
      (if left-button-down (left-click c x y) (right-click c x y)))
    (draw [c g x y elv]
      (let [ux (:x c)
            uy (:y c)]
        (do
          (if (and (= x ux) (= y uy))
            (ic.renderer/draw-cursor-icon g x y elv 0)))))  
    (update-command-state [c g]
      c)
    (apply-to-gui [c]
      (update-ui-for-selected-unit nil c))
    (get-command [c]
      nil))

; basic state with nothing selected
(defrecord NothingSelectedState []
  PCommandState
    (left-click [c x y]
      (let [u (get-unit (:game @state) x y)]
        (if (nil? u)
          c
          (selected-unit-state u x y nil))))
    (right-click [c x y]
      (let [u (get-unit (:game @state) x y)]
        (if (nil? u)
          c
          (view-unit-state u x y))))
    (list-click [c v]
      c)
    (mouse-dragged [c x y left-button-down right-button-down]
      c)
    (draw [c g x y elv]
      nil)
    (apply-to-gui [c]
      (update-ui-for-selected-unit nil c))
    (update-command-state [c g]
      c)
    (get-command [c]
      nil))


(defn selected-unit-state [u x y ability]
  (let [game (:game @state)
        targets (ic.units/get-ability-targets game u x y)
        target-map  
            (if (nil? ability)
							(target-map targets) 
							(target-map targets (fn [a] (= (:name a) (:name ability)))))
        sus (SelectedUnitState. nil {
                             :unit u 
                             :x x 
                             :y y 
                             :ability ability
                             :all-targets targets
                             :targets target-map
                             :costs (apcost-map targets target-map)})]
    (do 

;      (println sus)
      sus)))

(defn nothing-selected-state []
  (NothingSelectedState.))

(defn view-unit-state [u x y]
  (ViewUnitState. nil {:unit u :x x :y y}))

(defn set-command-state! [cs]
  "Sets the command state to a new command state"
  (let [ocs (:command-state @state)]
    (if (not= cs ocs)
      (do
        ;(if (not (nil? ocs)) (println (map-difference cs ocs)))
        ;(println cs)
        ;(println (:command-state @state))
			  (swap! state assoc :command-state cs)
			  (javax.swing.SwingUtilities/invokeLater 
			    (fn []
            ; apply command state to gui in event thread, re-reading latest value 
			      (apply-to-gui (:command-state @state))))))))

(defn update-current-command-state []
  "Updates the command state based on the latest game state"
  (let [s @state
        old-cs (:command-state s)
        game (:game s)
        new-cs (command-state-update old-cs game)]
    (swap! state assoc :command-state
      new-cs)
   (if (not= new-cs old-cs)
     (apply-to-gui (:command-state @state)))))

(def last-ui-state-game (atom nil))

(defn update-ui-state! []
  (javax.swing.SwingUtilities/invokeLater 
    (fn []
      (let [old-game @last-ui-state-game
            game (reset! last-ui-state-game (:game @state))
            game-changed (not= game old-game)]
	      (if game-changed (.repaint mini-map))
	      (update-current-command-state)
	      (.setEnabled button-next-turn (boolean (ic.game/side-has-human? (:game @state) (:moving-side @state))))))))

(swap! state assoc :command-state (nothing-selected-state))

; (def cs (:command-state @state))
; (def g (:game @state))
; (def ts (:targets cs))
; (def u (get-unit g 2 2))


; Component definitions

(defn mini-map-colour [g x y]
  (let [t (get-terrain g x y)
        u (get-unit g x y)]
    (cond 
      (nil? t)
        nil
      u
        (ic.units/side-colours (:side u))
      (or (:has-rail t) (:has-road t))
        (Color/GRAY)
      :default 
        (:map-colour t))))

(defn ^JComponent make-mini-map []
  (proxy [JComponent] []
    (paintComponent [^Graphics g]
      "Paint mini-map"
      (let [game (:game @state)
            ;bounds (.getClipBounds g)
            bounds (.getBounds ^JComponent this)
            x (.x bounds)
            y (.y bounds)
            w (.width bounds)
            h (.height bounds)
            xmin (int 0)
            ymin (int (- (/ w 2)))
            xmax (int (/ w 2))
            ymax (int (/ h 2))
            game (:game @state)] 
        (.setColor g Color/BLACK)
        (.fillRect g 0 0 w h)
        (dotimes [iy (- ymax ymin)]
		      (dotimes [ix (- xmax xmin)]
		        (let [tx (unchecked-add ix xmin)
                  ty (unchecked-add iy ymin)
                  c (mini-map-colour game tx ty)]
              (if c (do 
	              (.setColor g c)
	              (.fillRect g (* tx 2) (+ (* ty 2) tx) 2 2))))))
        (.setColor g Color/YELLOW)
        (let [viewport (.getViewport game-scrollpane)
              vpos (.getViewPosition viewport)
              vsize (.getExtentSize viewport)
              mx (mapx (.x vpos) (.y vpos))
              my (mapy (.x vpos) (.y vpos))
              mmw (* 2 (/ (.width vsize) ic.renderer/X_OFFSET))
              mmh (* 2 (/ (.height vsize) ic.renderer/Y_OFFSET))]
          (.drawRect g (* 2 mx) (+ (* 2 my) mx) mmw mmh))))))

(def ^JComponent mini-map
  (doto (make-mini-map)
    (.addMouseMotionListener
      (proxy [MouseMotionAdapter] []
		    (mouseDragged [^MouseEvent me]
		      (let [mx (. me getX)
		            my (. me getY)]
		            (mini-map-navigate mx my)))))
    (.addMouseListener
      (proxy [MouseAdapter] []
        (mousePressed [^MouseEvent me]
          (let [mx (. me getX)
                my (. me getY)]
                (mini-map-navigate mx my)))))))

(defn mini-map-navigate [mx my]
  (let [x (int (/ mx 2))
        y (int (/ (- my x) 2))]
    (scroll-to (ic.renderer/screenx x y) (ic.renderer/screeny x y))))

(defn ^JComponent make-gamescreen []
	(let [bstate (atom {:x 0 :y 0 :w 0 :h 0 :img nil :terrain nil})] 
    (proxy [JComponent] []
	    (paintComponent [^Graphics g]
        "Paint game screen, using a cached bufferedimage for the background terrain"
	      (let [bounds (.getClipBounds g)
	            x (.x bounds)
              y (.y bounds)
              w (.width bounds)
	            h (.height bounds)
	            xmin (dec (mapx x y))
	            ymin (mapy (+ x w) y)
	            xmax (+ 2 (mapx (+ x w) y))
	            ymax (inc (mapy x (+ y h)))
	            game (:game @state)] 
          (if (not (and (:img @bstate) (= w (:w @bstate)) (= h (:h @bstate)))) 
            (do
	            ;(println "Resizing terrain image: " w "*" h)
	            (swap! bstate 
	              (fn [s]
	                (merge s {:w w :h h :img (.createCompatibleImage (.getDeviceConfiguration ^Graphics2D g) w h ) :terrain nil})))))
          (if (not (and (= (:terrain @bstate) (get-map game)) (= x (:x @bstate)) (= y (:y @bstate))) )
            (do
              ;(println "Redrawing terrain: " w "*" h)
	            (let [bg (.getGraphics ^BufferedImage (:img @bstate))]
		            (.setColor bg Color/BLACK)
		            (.fillRect bg 0 0 w h)
		            (ic.renderer/draw-map bg game xmin ymin xmax ymax x y)
	              (swap! bstate
		              (fn [s]
		                (merge s {:x x :y y :terrain (get-map game)}))))))
          (.drawImage g (:img @bstate) x y nil)
	        (ic.renderer/draw-all-objects g game xmin ymin xmax ymax (:command-state @state)))))))

(def ^JComponent gamescreen (make-gamescreen))


(defn handle-drag [x y left-button right-button]
  "Handle a drag, return true if acted on by command state"
  (let [cs (:command-state @state)
        ncs (mouse-dragged cs x y left-button right-button)
        cmd (get-command ncs)]
  ; todo execute command if not nil
   (do
     ; (println (str "Click at: " x "," y " -> " ncs))
     ;(println "Mouse dragged!")
     (set-command-state! ncs ))
   (not= cs ncs)))

(def last-mouse-pos (atom (Point. 0 0)))

(defn ^MouseMotionAdapter make-mouse-motion-adapter []
  (proxy [MouseMotionAdapter] []
    (mouseMoved [^MouseEvent me]
      (let [mx (. me getX)
            my (. me getY)
            mtx (mapx mx my)
            mty (mapy mx my)]
       ; (println (str "mx,my = " mx "," my  "  &  mtx,mty = " mtx "," mty))
        (swap! state assoc :mouseover [mtx mty])
        (reset! last-mouse-pos (Point. mx my))))
    (mouseDragged [^MouseEvent me]
      (let [mx (. me getX)
            my (. me getY)
            mtx (mapx mx my)
            mty (mapy mx my)
            button (.getButton me)
            modifiers (.getModifiersEx me)
            handled? (handle-drag 
              mtx mty 
              (> (bit-and modifiers MouseEvent/BUTTON1_DOWN_MASK) 0) 
              (> (bit-and modifiers MouseEvent/BUTTON3_DOWN_MASK) 0))]          
          (if (and (not handled?) (not (instance? PainterState (:command-state @state))))
              ; do scrolling, adjusting for view position
              (let [viewport (.getViewport game-scrollpane)
                    lpos @last-mouse-pos
                    spos (.getViewPosition viewport)
                    nx (- (.x spos) (- mx (.x lpos)))
                    ny (- (.y spos) (- my (.y lpos)))]
                (scroll-to nx ny)))))))

(defn handle-left-click [x y]
  (let [cs (:command-state @state)
        ncs (left-click cs x y)
        cmd (get-command ncs)]
  ; todo execute command if not nil
   (do
     ;(println (str "Click at: " x "," y " -> " ncs))
     (set-command-state! ncs ))))

(defn handle-right-click [x y]
  (let [cs (:command-state @state)
        ncs (right-click cs x y)
        cmd (get-command ncs)]
   (do
     (set-command-state! ncs ))))

(defn ^MouseAdapter make-mouse-adapter []
  (proxy [MouseAdapter] []
    (mouseClicked [^MouseEvent me]
      (let [button (.getButton me)
            [x y] (@state :mouseover)]
			   ;(println (str "Mouse pressed: " x "," y " : " (.toString me)))
         (case button
			     1
				     (handle-left-click x y)
           3
             (handle-right-click x y)
			     (println (str "Button pressed: " button)))))))

; UI Components


(def ^JPanel right-panel (JPanel.))

(def ^JPanel left-panel (JPanel.))



(def ^JCheckBox button-paint  
  (doto (JCheckBox. "Show editing controls")
    (.addActionListener 
      (proxy [ActionListener] []
        (actionPerformed [^ActionEvent e]
          (if (.isSelected button-paint)
            (do
		          (ic.dialogs/show-paint-dialog)
		          (set-command-state! (PainterState.)))
            (do
              (set-command-state! (nothing-selected-state)))))))))

(swap! ic.dialogs/button-paint (fn [old] button-paint))


(def ^JButton button-ai-move 
  (doto (JButton. "Green AI: off")
    (.addActionListener 
      (proxy [ActionListener] []
        (actionPerformed [^ActionEvent e]
          (swap! state
            (fn [s]
              (let [game (:game s)
                    player (ic.game/get-player-for-side game 0)
                    aim (not (:ai-move s))]
                (.setText button-ai-move (str (if aim "Green AI: on" "Green AI: off")))
                (merge s 
                  {:game (if player (update-player game (assoc player :ai-controlled aim)) game)
                   :ai-move aim})))))))))


(def ^JTextArea unit-description-panel 
  (doto
    (JTextArea. (unit-description-text nil))
    (.setEditable false)
    (.setLineWrap true)
    (.setWrapStyleWord true)))


(def ^JTextArea unit-status-panel 
  (doto 
    (JTextArea. (unit-status-text nil))
    (.setEditable false)
    (.setLineWrap true)
    (.setWrapStyleWord true)))

(def ^JTextArea mission-panel 
  (doto
    (JTextArea. "Your mission:\n1. Destroy the red units.\n2. Protect your vulnerable Crawler.\n")
    (.setEditable false)
    (.setLineWrap true)
    (.setWrapStyleWord true)))


; ===============================================================
; game loading 
(defn update-game [game]
  (swap! state 
    (fn [oldstate]
      (assoc oldstate :game game))))

(defn load-game [g] 
  (swap! state
    (fn [s]
      (merge s 
        {:game g
         :playing true
         :moving-side 0
         :player-id (:id (ic.game/get-player-for-side g 0))
         :command-state (nothing-selected-state)})))
  (update-gamescreen-bounds)
  (scroll-to 0 0) ; in case bounds have changed
  (update-ui-state!) 
  (.setText mission-panel (or (:mission-text g) "No mission details available.")))

(def file-directory (atom (File. ".")))

(def ^JButton button-save-game  
  (doto (JButton. "Save Game...")
    (.addActionListener 
      (proxy [ActionListener] []
        (actionPerformed [^ActionEvent e]
          (let [^JFileChooser fc (JFileChooser. @file-directory)
                result (.showSaveDialog fc ic.frame/frame)]
            (if (= (JFileChooser/APPROVE_OPTION) result)
              (let [file (.getSelectedFile fc)]
                (if 
                  (or 
                    (not (.exists file)) 
                    (= (JOptionPane/OK_OPTION) (JOptionPane/showConfirmDialog ic.frame/frame "Are you sure you want to overwrite this file?" "Confirm file overwrite" (JOptionPane/OK_CANCEL_OPTION))))
                  (Tools/writeStringToFile file (ic.serial/serialize (:game @state))))
                ))
            (reset! file-directory (.getCurrentDirectory fc))))))))

(def ^JButton button-load-game  
  (doto (JButton. "Load Game...")
    (.addActionListener 
      (proxy [ActionListener] []
        (actionPerformed [^ActionEvent e]
          (let [^JFileChooser fc (JFileChooser. @file-directory)
                result (.showOpenDialog fc ic.frame/frame)]
            (if (= (JFileChooser/APPROVE_OPTION) result)
              (let [file (.getSelectedFile fc)
                    game (ic.serial/load-game-from-file file)]
                (load-game game)))
            (reset! file-directory (.getCurrentDirectory fc))))))))

(defn build-new-map [size]
  (let [g (ic.gamefactory/make-game size)]
     (load-game g)))

(defn ^JButton button-new-map [size] 
  (doto (JButton. (str "New Map:  " size " x " size))
    (.addActionListener 
      (proxy [ActionListener] []
        (actionPerformed [^ActionEvent e]
          (build-new-map size))))))




; Timer for animations

(defn timer-loop []
  (do
    (let [millis (System/currentTimeMillis)
          lastMillis @ic.renderer/anim-millis
          dt (float (/ (- millis lastMillis) 1000))]

      (reset! ic.renderer/anim-millis millis)
      (swap! ic.renderer/anim-time #(float (mod (+ % (/ dt ic.renderer/ANIM_CYCLE)) 1.0)))
     ; (println (str "timer event!! anim-time = " @anim-time "  icon = " (icon-anim-pos)))        
      (.repaint gamescreen))
    (Thread/sleep 30)
    (recur)))

; Thread for running commands and game screen interactions
(def ^Thread timer-thread 
  (Thread. timer-loop))

; ==========================================================================================
; GUI construction

(def my-layout 
  (MigLayout.
    "insets 0 0 0 0"
    "[250!][200:2000:][250!]"
    "[50!][100:2000:][50!]"))

(defn ^JScrollPane make-game-scrollpane [screen]
  (let [scrollpane (JScrollPane. screen
                     (JScrollPane/VERTICAL_SCROLLBAR_ALWAYS)
                     (JScrollPane/HORIZONTAL_SCROLLBAR_ALWAYS))]
    (doto scrollpane
      (.setBorder (mikera.ui.steampunk.SteamPunkBorder. true)))
    scrollpane))

(def game-scrollpane (make-game-scrollpane gamescreen))

(defn scroll-to [x y]
  (let [viewport (.getViewport game-scrollpane)
        viewsize (.getViewSize viewport)
        vpsize (.getExtentSize viewport)
        vx (middle 0 x (- (.width viewsize) (.width vpsize)))
        vy (middle 0 y (- (.height viewsize) (.height vpsize)))
        point (Point. vx vy)]
    (.setView viewport gamescreen)
    (.setViewPosition viewport point)
    (.repaint mini-map)))

(defn ^Rectangle game-bounds [game]
  (let [xmin (atom 1000000)
        ymin (atom 1000000)
        xmax (atom -1000000)
        ymax (atom -1000000)    
        terrain (get-map game)]
    (mvisit terrain
      (fn [x y t]
        (let [sx (ic.renderer/screenx x y)
              sy (ic.renderer/screeny x y)]
          (if (< sx @xmin) (reset! xmin sx))
          (if (< sy @ymin) (reset! ymin sy))
          (if (> sx @xmax) (reset! xmax sx))
          (if (> sy @ymax) (reset! ymax sy)))))
    (Rectangle. 
      (- @xmin 25) 
      (- @ymin 25) 
      (+ (- @xmax @xmin) 110) 
      (+ (- @ymax @ymin) 110))))

(defn update-gamescreen-bounds []
  (let [gbounds (game-bounds (:game @state))]
    (doto gamescreen
;      (.setBounds gbounds)
      (.setMinimumSize (.getSize gbounds))
      (.setPreferredSize (.getSize gbounds)))))

; =============================================================================================
; Bottom buttons
; 

(def ^JButton button-next-turn  
  (doto (ic.ui/make-title-button "Finish Turn")
    (.addActionListener 
      (proxy [ActionListener] []
        (actionPerformed [^ActionEvent e]
          (send-command (ic.command/next-turn)))))))

(def ^JButton button-exit  
  (doto (ic.ui/make-title-button "Return to base")
    (.addActionListener 
      (proxy [ActionListener] []
        (actionPerformed [^ActionEvent e]
          (if (= 
                (JOptionPane/OK_OPTION) 
                (JOptionPane/showConfirmDialog ic.frame/frame "Are you sure you want to leave this mission and return to base?" "Exit current game?" (JOptionPane/OK_CANCEL_OPTION)))
            (@exit-function)))))))

(def bottom-button-panel
  (let [outer-panel (JPanel.)
        panel (JPanel.)] 
    (doto outer-panel
      (.add panel))
    (doto panel
      (.setLayout (GridLayout. 1 3)))
    (.add panel (JPanel.))
    (.add panel button-next-turn)
    (.add panel button-exit)
    outer-panel))



(defn setup-screen [mode]
  "Set up the game screen for the desired game mode"
  (let []	
    (case mode
	    "Play"
	      (let []
         (doto left-panel
		        (.removeAll)
            (.add (ic.ui/panelize mini-map) "dock north")
            (.add (JSeparator.) "dock north")
		        (.add (ic.ui/panelize mission-panel) "dock north")
		        (.add (JSeparator.) "dock north"))
          (swap! state assoc :playing true))
	      
	    "Paint"
        (let []
		      (doto left-panel
	          (.removeAll)
            (.add (ic.ui/panelize mini-map) "dock north")
            (.add (JSeparator.) "dock north")
				    (.add (JPanel.) "dock north")
				    (.add button-paint "dock north")
				    (.add (JPanel.) "dock north")
				    (.add button-ai-move "dock north")
				    (.add (JPanel.) "dock north")
				    (.add button-save-game "dock north")
				    (.add button-load-game "dock north")
            (.add (JPanel.) "dock north")
				    (.add (button-new-map 10) "dock north")
		        (.add (button-new-map 20) "dock north")
            (.add (button-new-map 30) "dock north")
            (.add (button-new-map 50) "dock north")
		        (.add (JPanel.) "dock north"))
        (swap! state assoc :playing false)))
    (update-ui-state!)))

(defn launch-window [^JPanel panel]
  (.setPreferredSize mini-map (Dimension. 200 150)) 
  (set-command-state! (nothing-selected-state))  
  (doto gamescreen
    (.addMouseMotionListener (make-mouse-motion-adapter))
    (.addMouseListener (make-mouse-adapter)))
  (doto panel
    (.setLayout my-layout)	      
    (.add game-scrollpane  "cell 1 1 1 1, grow")
    (.add right-panel "cell 2 1 1 1, grow")
    (.add left-panel "cell 0 1 1 1, grow")
    (.add bottom-button-panel "cell 0 2 3 1, grow"))
  (doto left-panel
    (.setLayout (MigLayout. "wrap 1, insets 0 0 0 0", "[250!]")))
  (doto right-panel
    (.setLayout (MigLayout. "wrap 1, insets 0 0 0 0", "[250!]"))
    (.add (ic.ui/panelize unit-description-panel) "dock north")
    (.add (ic.ui/panelize unit-status-panel) "dock north")
    (.add ability-list "dock north"))
  (setup-screen "Paint")
  (doto button-next-turn
    (.requestFocus))
  (doto timer-thread
    (.start))
  (doto game-thread
    (.start))
  (doto engine-thread
    (.start)))

(load-game (ic.gamefactory/random-terrain-game 10))
