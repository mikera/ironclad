(ns ic.protocols)

(defprotocol PAbility 
  (get-targets [ability game unit ^int x ^int y])
  (ai-evaluate [ability game unit ^int sx ^int sy ^int tx ^int ty apcost])
  (ai-action-param [ability game unit ^int sx ^int sy ^int tx ^int ty])
  (needs-param? [ability])
  (apply-to-game [ability game unit ^int sx ^int sy ^int tx ^int ty command])
  (^javax.swing.Icon ability-icon [ability])
  (^String nameString [ability unit]))

 
(defprotocol PMap
  "Abstraction for hexagonal map"
  (mget [m ^int x ^int y])
  (mset [m ^int x ^int y v])
  (mvisit [m f])
  (mmap [m f])
  (mmap-indexed [m f]))


(defprotocol PLocation
  "Abstraction for hexagonal map locations"
  (get-x [p])
  (get-y [p])
  (add [p q])
  (adjacents [p]))

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
  (get-unit [g ^int x ^int y] [g ^int uid])
  (get-terrain [g ^int x ^int y])
  (set-terrain [g ^int x ^int y ^ic.map.Terrain t])
  (get-unit-map [g])
  (add-unit [g ^int x ^int y u])
  (add-player [g p])
  (get-player [g ^int player-id])
  (update-player [g p])
  (remove-unit [g ^int x ^int y]))

(defprotocol PDrawable
  "Abstraction for drawable"
  (^Integer sourcex [d])
  (^Integer sourcey [d])
  (^Integer sourcew [d])
  (^Integer sourceh [d])
  (^Integer centrex [d])
  (^Integer centrey [d])
  (^java.awt.image.BufferedImage source-image [d])
  (^javax.swing.Icon drawable-icon [d]))


(defprotocol PCommandState
  "State and transition management for the GUI command interface. Each event updates the command state. 
   When a command is complete it can be accessed with get-command"
  (left-click [c x y])
  (right-click [c x y])
  (mouse-dragged [c x y left-button-down right-button-down])
  (list-click [c v])
  (update-command-state [c ^Game g])
  (draw [c ^Graphics g x y elv])
  (apply-to-gui [c])
  (get-command [c]))
 


