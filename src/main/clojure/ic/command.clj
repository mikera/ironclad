(ns ic.command)

(defrecord Command [
   ^String command-type
   ^long uid
   ^String ability
   ^long tx
   ^long ty
   ^Object param]
  Object
    (toString [self] (str (merge {} self))))

(defn command [uid ability tx ty param]
  (Command. 
     "Command"
     uid 
     ability 
     tx 
     ty
     param))

(defn god-command [update]
  {:command-type "God-Command" 
   :update update})

(defn time-event [millis base]
  {:command-type "Tick tock" 
   :millis millis
   :base base})

(defn next-turn []
  {:command-type "Next turn"})


; unit update messages

(def MSG_UNIT_PROPERTY "Unit property")
(def MSG_UNIT_PROPERTIES "Unit properties")
(def MSG_PLAYER_PROPERTIES "Player properties")
(def MSG_REMOVE_UNIT "Remove unit")
(def MSG_ADD_UNIT "Add unit")
(def MSG_SET_TERRAIN "Set terrain")
(def MSG_MOVE_UNIT "Move unit")
(def MSG_FAILED_COMMAND "Failed command")
(def MSG_NEXT_TURN "Next turn")

(defn msg-update-unit 
  ([u1 prop value]
    {:update-type MSG_UNIT_PROPERTY
     :property prop
     :uid (:id u1)
     :value value})
  ([u1 props]
    {:update-type MSG_UNIT_PROPERTIES
     :uid (:id u1)
     :properties props}))

(defn msg-update-player 
  ([player props]
    {:update-type MSG_PLAYER_PROPERTIES
     :player-id (:id player)
     :properties props}))

(defn msg-next-turn []
  {:update-type MSG_NEXT_TURN})

(defn msg-remove-unit [u1 tx ty]
  {:update-type MSG_REMOVE_UNIT
   :uid (:id u1)
   :tx tx
   :ty ty})

(defn msg-set-terrain [^Integer tx ^Integer ty ^ic.map.Terrain t]
  {:update-type MSG_SET_TERRAIN
   :terrain t
   :tx tx
   :ty ty})

(defn msg-move-unit [u1 sx sy tx ty]
  {:update-type MSG_MOVE_UNIT
   :uid (:id u1)
   :sx sx
   :sy sy
   :tx tx
   :ty ty})

(defn msg-add-unit [u1 tx ty]
  {:update-type MSG_ADD_UNIT
   :unit u1
   :tx tx
   :ty ty})

(defn msg-command-fail [s]
  {:update-type MSG_FAILED_COMMAND
   :message s})