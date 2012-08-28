(try
  (set! *warn-on-reflection* true)
  (catch Throwable t nil))

(ns ic.main
  (:use [ic.frame])
  (:use [mc.util])
  (:use [clojure.test])
  (:require [ic.interface])
  (:require [ic.ui])
  (:import (javax.swing JLabel JScrollPane JCheckBox JTextPane ListModel SwingUtilities DefaultListModel JFrame JSeparator JComponent JList JTextField JTextArea JButton JPanel BoxLayout BorderFactory Timer)
           (java.awt Insets GridLayout BorderLayout FlowLayout Rectangle Dimension Graphics Color CardLayout)
           (java.awt.event ActionListener MouseAdapter MouseMotionAdapter MouseEvent)
           (ic IronClad)
           (mikera.util Tools)
           (mikera.ui.steampunk SteamPunkBorder PanelBorder SteamPunkStyle)
           (mikera.ui JIcon)
           (mikera.web WebUtils)))


(defn switch-to-screen [name]
  (.show card-layout (.getContentPane frame) name))


(defn play-game [game]
  (let []
    (reset! ic.interface/exit-function #(switch-to-screen "Title"))
    (ic.interface/load-game game)
    (ic.interface/setup-screen "Play")
    (switch-to-screen "Game Screen")))


(defn ^JPanel make-sub-screen-buttons []
  (let [exit-button (ic.ui/make-title-button "Return to main menu" #(switch-to-screen "Title"))]
    (ic.ui/make-button-panel [exit-button])))

(defn ^JPanel make-sub-screen [panel]
  "Makes a sub screen with the standard format / return to main title at the bottom"
  (let [screen (JPanel.)]
	  (doto screen
	    (.setLayout (BorderLayout.)) 
      (.add panel (BorderLayout/CENTER) )
      (.add (make-sub-screen-buttons) (BorderLayout/SOUTH) ))))

(def ^JButton play-button 
  (ic.ui/make-title-button 
		"Play Random Map" 
		#(play-game (ic.gamefactory/random-challenge-game))))

(def tutorial-list 
  [["Training Grounds" 
    "Welcome to your new posting, Leuitenant!

To help you familiarize yourself with this territory and the troops under your command, we have prepared a short set of maneuveres and target practice for your troops. 

Nice day for a bit of shooting! See you back at base for tea and cake."
    
    "Your mission: 

1. Destroy all the red targets


Hints:
1. The flashing white bars on your units show they they have action points and are ready to move

2. Click on a unit to select it and see possible moves

3. Click on the blue movement hexagons to move your units 

4. When near a target, attack it by clicking the red hexagon

5. Press 'Finish Turn' once you have finished moving

6. Your units will recieve new action points to move in the next turn
"
    ]
   ["Rebel skirmish"
    "As you may well know, we have been seeing increased rebel activity in this area.  Our scouts have sighted a rebel encampment in the hills.  They have launched a number of observation balloons, suggesting that they are preparing for an offensive.

You must destroy the enemy forces and shoot down their balloons. You'll be needing some firepower, so we've provided you with a Battle Tank to eliminate any entrenched enemy positions."
    "Your mission: 

1. Destroy the rebel forces


Hints:

1. The enemy forces will move after you finish your turn

2. Try to position your troops so that you are able to attack first

3. If your units are damaged, you will see a a green bar that shows how many hit points are remaining. It's a good idea to retreat heavily damaged units.

4. Your Battle Tank has a powerful cannon that can fire two hexes, however it requires two action points to fire.

5. Use terrain to your advantage! Infantry are very effective at defending trenches, woods and hilly terrain, but are vulnerable in open fields.

6. The enemy pillbox might be tough to destroy, and has a firing range of two hexes - so it is best to plan a well-coordinated attack
"]
   ["Forest rescue" 
    "Our fears of a rebel offensive were true!  We have recieved unfortunate news that rebel forces have attacked and captured a village in the forests of the SouthEast sector.  It is your task to drive them out and recapture the village before they commit any more atrocities.\n\nLuckily, we have been able to obtain some artillery support from the 5th Battalion.  We have confidence in your ability to achieve victory!"
    "Your mission: 

1. Destroy the rebel forces

2. Recapture the village with your riflemen


Hints:

1. Keep your Artillery Tanks safe, and use them to attack at range (2-4 hexes). 

2. Artillery can either move or fire, but not both in the same turn.

3. Capture buildings by moving your riflemen into them

4. To get riflemen out of a building once it is captured, click on the building then click on an adjacent square to deploy the unit.
"
    ]
   ["Battle of Ilerium Bay" 
    "War is upon us!  The perfidious Krantz Empire has declared its support for the rebels.  We have recieved news that a large rebel force is heading towards Illum Bay, and that they have been equipped with heavy weapons by Krantz agents. 

You must defend this strategic bay at all costs.  You will have command of the town's defences and the small naval sqaudron that is stationed there.  Good fortune be with you, and fight bravely for Albion!"
    "Your mission: 

1. Defend the town at Illum Bay and destroy the attacking rebel forces


Hints: 

1. You seem to be outnumbered, so choose a good place to defend where the enemy cannot overrun you. It might be wise to fall back to a strong defensive position at first.

2. Your Paddle Cruiser has heavy cannons, ideal for bombarding enemy armoured units. Position it carefully to inflict the greatest damage to your enemies.

3. Watch out for the enemy Artillery! They have a firing range of five hexes.
"]])




(defn play-tutorial [number]
  (let [filename (str "maps/tutorial-" number ".gam")
        game (ic.serial/load-game-from-resourcename filename)]
    (play-game (assoc game :mission-text (last (nth tutorial-list number))))
    (reset! ic.interface/exit-function #(switch-to-screen "Tutorial Menu"))))

(defn ^JPanel make-tutorial-list []
  "Creates a panel containing the selectable list of tutorials"
  (let [panel (JPanel.)]
    (.setLayout panel (GridLayout. 0 1))
    (doseq [i (range 0 (count tutorial-list))]
      (let [sub-panel (JPanel.)
            button-panel (JPanel.)
            [name description mission-text] (nth tutorial-list i)
            button (ic.ui/make-title-button name #(play-tutorial i))
            label (JTextArea. description)]
;        (.setForeground label (SteamPunkStyle/GOLD_COLOUR))
				(doto label
			    (.setEditable false)
			    (.setLineWrap true)
			    (.setWrapStyleWord true))
				(.setPreferredSize sub-panel (Dimension. 750 180))
        ;(.setFont label (SteamPunkStyle/LARGE_FONT))
        (.setLayout sub-panel (BorderLayout.))
        (.add button-panel button)
        (.add sub-panel button-panel (BorderLayout/WEST))
        (.add sub-panel label (BorderLayout/CENTER))
        (.setBorder sub-panel (mikera.ui.steampunk.PanelBorder/FILLED_BORDER))
        (.add panel sub-panel)))
    panel))

(def ^JButton tutorial-button 
  (ic.ui/make-title-button 
    "Tutorial" 
    #(do 
       (switch-to-screen "Tutorial Menu"))))

(def ^JButton map-editor-button 
  (ic.ui/make-title-button "Map Editor" 
    #(do 
      (ic.interface/setup-screen "Paint")
      (ic.interface/load-game (ic.gamefactory/make-game))
	    (switch-to-screen "Game Screen"))))

(def ^JButton website-button 
  (ic.ui/make-title-button "Go to Ironclad website" 
    #(do 
      (WebUtils/launchBrowser "http://www.mikera.net/ironclad/"))))

(def ^JPanel tutorial-screen
  (let [tpanel (JPanel.)]
    (doto tpanel
      (.add (make-tutorial-list) (BorderLayout/CENTER)))
    (make-sub-screen tpanel)))

(def ^JPanel title-buttons 
  (ic.ui/make-button-panel
    [tutorial-button
     play-button
     map-editor-button
     website-button]))

(def ^JPanel title-image
  (let [icon-panel (JPanel.)
        icon (JIcon. ic.graphics/title-image)]
;    (doto icon
;      (.setBorder (mikera.ui.steampunk.SteamPunkBorder/UNFILLED_BORDER)))
    (doto icon-panel
      (.setLayout (BoxLayout. icon-panel (BoxLayout/Y_AXIS)))
      (.setAlignmentY (java.awt.Component/CENTER_ALIGNMENT))
      (.add icon))
    icon-panel))

(def ^JPanel title-screen 
  (let [panel (JPanel.)] 
    (doto panel
      (.setLayout (BorderLayout.))
      (.add title-image BorderLayout/CENTER)
      (.add title-buttons BorderLayout/SOUTH))
    
    panel))

(def ^JPanel game-screen (JPanel.))

(def return-to-title-function
  #(switch-to-screen "Title"))

(defn- main []
  "Main function"
  (javax.swing.SwingUtilities/invokeLater 
    (fn []
      (doto ^JComponent (.getContentPane frame)
        (.add title-screen "Title")
        (.add game-screen "Game Screen")
        (.add tutorial-screen "Tutorial Menu")
        (.validate))
      (switch-to-screen (if (IronClad/START_SCREEN) "Title" "Game Screen"))
      (reset! ic.interface/exit-function return-to-title-function)
      (ic.interface/launch-window game-screen)
      (ic.sounds/play "Fanfare")))) 

(do 
  (run-all-tests (re-pattern "ic.*"))
  (run-all-tests (re-pattern "mc.*"))
  (main))

(ns ic.interface)

