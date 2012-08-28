(ns ic.player)

(defrecord Player [])

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

