(ns ic.protocols)

(defprotocol PAbility 
  (get-targets [ability game unit x y])
  (ai-evaluate [ability game unit sx sy tx ty apcost])
  (ai-action-param [ability game unit sx sy tx ty])
  (needs-param? [ability])
  (apply-to-game [ability game unit sx sy tx ty command])
  (^javax.swing.Icon ability-icon [ability])
  (^String nameString [ability unit]))

(defprotocol PLocationSet
  "Abstraction for sets of locations"
  (get-points [p])
  (union [p q])
  (intersection [p q])
  (expand [p]))

(defprotocol PGame 
  "Abstraction for complete game state"
  (get-updates [g event])
  (update [g update])
  (get-map [g])
  (^ic.map.Point location-of-unit [g u])
  (get-unit [g ^long x ^long y] [g ^long uid])
  (get-terrain [g ^long x ^long y])
  (set-terrain [g ^long x ^long y ^ic.map.Terrain t])
  (get-unit-map [g])
  (add-unit [g ^long x ^long y u])
  (add-player [g p])
  (get-player [g ^long player-id])
  (update-player [g p])
  (remove-unit [g ^long x ^long y]))

(defprotocol PDrawable
  "Abstraction for drawable"
  (sourcex ^long [d])
  (sourcey ^long [d])
  (sourcew ^long [d])
  (sourceh ^long [d])
  (centrex ^long [d])
  (centrey ^long [d])
  (source-image ^java.awt.image.BufferedImage [d])
  (drawable-icon ^javax.swing.Icon [d]))


(defprotocol PCommandState
  "State and transition management for the GUI command interface. Each event updates the command state. 
   When a command is complete it can be accessed with get-command"
  (left-click [c ^long x ^long y])
  (right-click [c ^long x ^long y])
  (mouse-dragged [c ^long x ^long y left-button-down right-button-down])
  (list-click [c v])
  (update-command-state [c ^Game g])
  (draw [c ^Graphics g ^long x ^long y elv])
  (apply-to-gui [c])
  (get-command [c]))
 


