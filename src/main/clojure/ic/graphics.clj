(ns ic.graphics
  "Loader code for graphics files"
  (:require [mc.resource])
  (:use [ic.protocols])
  (:import  [mikera.ui.steampunk Images]))

(def ^java.awt.image.BufferedImage unit-image
  (mc.resource/load-image "pov/units.png"))

(def ^java.awt.image.BufferedImage title-image
  (mc.resource/load-image "pov/title.png"))
 
(def ^java.awt.image.BufferedImage terrain-image
  (mc.resource/load-image "pov/terrain.png"))

(def ^java.awt.Font main-font 
  (.deriveFont
    (mc.resource/load-font "fonts/FINALOLD.TTF")
    (float 20.0)))

(def ^java.awt.Font mini-font 
  (.deriveFont
    main-font
    (float 13.0)))


(def ^java.awt.image.BufferedImage icon-image
  (mc.resource/load-image "pov/icons.png"))

(def ^java.awt.image.BufferedImage wood-image
  mikera.ui.steampunk.Images/WOOD)



