(ns ic.sounds
  "Sound effects and sound utilities"
  (:require [mc.resource])
  (:import [mikera.sound Sample SoundEngine])
  (:use [ic.protocols]))

(set! *warn-on-reflection* true)
(set! *unchecked-math* true)

(def sample-map (atom {}))

(def resource-map {
  "Explosion" "sounds/explosion-1.au"
  "Rifle fire" "sounds/rifle-burst.au"
  "Cannon fire" "sounds/big-shot.au"
  "Death ray fire" "sounds/lazer-1.au"
  "Fanfare" "sounds/victory-fanfare.au"})

(def SOUNDS_ENABLED true)

(defn load-sample [samplename]
  (let [resourcename (resource-map samplename)
        sample (SoundEngine/loadSample resourcename)]
    (swap! sample-map assoc samplename sample)
    sample))

(defn get-sample [effectname]
  (if-let [sample ^Sample (@sample-map effectname)]
    sample
    (load-sample effectname)))

(defn play [effectname]
  (if SOUNDS_ENABLED
    (let [sample ^Sample (get-sample effectname)]
      (.play sample))))

