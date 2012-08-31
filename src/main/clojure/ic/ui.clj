(ns ic.ui
  (:use [ic.protocols])
  (:use [mc.util])
  (:require [mc.resource])
  (:require [mc.ui])
  (:require [ic.game])
  (:require [ic.command])
  (:require [ic.dialogs])
  (:require [ic.gamefactory])
  (:require [ic.map])
  (:require [ic.renderer])
  (:require [ic.serial])
  (:require [ic.units])
  (:import (javax.swing JScrollPane JCheckBox JOptionPane JFileChooser JTextPane ListModel SwingUtilities DefaultListModel JFrame JSeparator JComponent JList JTextField JTextArea JButton JPanel BoxLayout BorderFactory Timer)
           (java.awt.event ActionListener MouseAdapter MouseMotionAdapter MouseEvent)
           (java.awt.image BufferedImage)
           (java.net URL)
           (java.io File)
           (javax.swing.event ListSelectionListener ListSelectionEvent)
           (java.awt GraphicsConfiguration Graphics2D GridLayout BorderLayout FlowLayout Rectangle Dimension Graphics Color)
           (mikera.engine Hex)
           (mikera.util Tools)
           (mikera.ui.steampunk PanelBorder SteamPunkStyle)
           (ic ListCellData)
           (net.miginfocom.swing MigLayout)))


(defn make-action-listener [f]
  (proxy [ActionListener] []
    (actionPerformed [^ActionEvent e]
      (f))))

(defn ^JPanel panelize [^JComponent comp]
  (doto 
    (JPanel.)
    (.setLayout (BorderLayout.))
    (.setBorder (mikera.ui.steampunk.PanelBorder/FILLED_BORDER))
    (.add comp "Center" )))

(defn make-button-panel [buttons]
  (let [outer-panel (JPanel.)
        panel (JPanel.)] 
    (doto outer-panel
      (.add panel))
    (doto panel
      (.setLayout (GridLayout. 0 1)))
    (doseq [^JComponent button buttons]
      (.add panel button))
    outer-panel))

(defn make-title-button 
  ([string] 
    (doto (JButton. string)
      (.setFont (SteamPunkStyle/LARGE_FONT))
      (.setPreferredSize (Dimension. 250 40))))
  ([string actionfn] 
    (doto (make-title-button string)
      (.addActionListener (make-action-listener actionfn)))))
