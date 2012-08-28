(ns ic.frame
    (:import (javax.swing JComponent JFrame JLabel JPanel UIManager)
             (javax.swing.plaf.synth SynthLookAndFeel)
             (java.awt Component Insets GridLayout BorderLayout FlowLayout Rectangle Dimension Graphics Color CardLayout)
             (mikera.ui.steampunk SteamPunkStyleFactory SteamPunkStyle)
             (mikera.ui Tools)
             (ic IronClad)))

(do 
  (javax.swing.UIManager/setLookAndFeel (SynthLookAndFeel.))
  (javax.swing.plaf.synth.SynthLookAndFeel/setStyleFactory
    (SteamPunkStyleFactory.)))

(def ^JPanel loading-screen
  (let [panel (JPanel.)
        label (JLabel. "Loading...")]
    (.setLayout panel (BorderLayout.))
    (doto label
      (.setForeground (SteamPunkStyle/GOLD_COLOUR))
      (.setFont (SteamPunkStyle/HUGE_FONT)))
    (.setVerticalAlignment label (Component/CENTER_ALIGNMENT))
    (.setHorizontalAlignment label (Component/CENTER_ALIGNMENT))
    (.setVerticalTextPosition label (Component/CENTER_ALIGNMENT))
    (.setHorizontalTextPosition label (Component/CENTER_ALIGNMENT))
    (.add panel label (BorderLayout/CENTER))
    panel))

(def ^CardLayout card-layout (CardLayout.))


(def ^JFrame frame 
  (if (ic.IronClad/applet) 
    ic.IronClad/applet
    (let [newframe (JFrame. "Ironclad: Steam Legions")
          screensize (mikera.ui.Tools/getScreenSize)
          fw (min 1280 (.width screensize))
          fh (min 960 (.height screensize))]
      (doto newframe
			  (.setDefaultCloseOperation JFrame/EXIT_ON_CLOSE)
			  (.setSize (Dimension.  fw fh)) 
			  (.setVisible true))
      (doto ^JComponent (.getContentPane newframe)
        (.setLayout card-layout)
        (.add loading-screen "Loading Screen"))
      (.show card-layout (.getContentPane newframe) "Loading Screen")
      newframe)))



