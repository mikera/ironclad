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
  (get-map [g])
  (get-unit-map [g])
  (add-player [g p])
  (get-player [g ^long player-id])
  (update-player [g p]))

(defprotocol PDrawable
  "Abstraction for drawable"
  (sourcex [d])
  (sourcey [d])
  (sourcew [d])
  (sourceh [d])
  (centrex [d])
  (centrey [d])
  (source-image [d])
  (drawable-icon [d]))


(defprotocol PCommandState
  "State and transition management for the GUI command interface. Each event updates the command state. 
   When a command is complete it can be accessed with get-command"
  (left-click [c x y])
  (right-click [c x y])
  (mouse-dragged [c x y left-button-down right-button-down])
  (list-click [c v])
  (update-command-state [c g])
  (draw [c g x y elv])
  (apply-to-gui [c])
  (get-command [c]))

(defprotocol PValidatable
  (validate [x]))
 


