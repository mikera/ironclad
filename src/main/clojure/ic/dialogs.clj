(ns ic.dialogs
  "Dialog boxes"
  (:use [ic protocols engine map units game gamefactory])
  (:use [mc.util])
  (:require [mc.resource])
  (:require [mc.ui])
  (:require [ic.frame])
  (:use [ic.command])
  (:import (javax.swing JScrollPane JCheckBox JTabbedPane JDialog ListSelectionModel JList JTextPane SwingUtilities DefaultListModel JFrame JSeparator JComponent JList JTextField JTextArea JButton JPanel BoxLayout BorderFactory Timer)
           (java.awt.event ActionListener ActionEvent WindowAdapter WindowEvent MouseAdapter MouseMotionAdapter MouseEvent)
           (java.net URL)
           (javax.swing.event ListSelectionListener ListSelectionEvent ChangeListener)
           (javax.swing.border EmptyBorder)
           (java.awt GridLayout BorderLayout FlowLayout Dimension Graphics Color)
           (mikera.engine Hex)
           (mikera.ui.steampunk PanelBorder)
           (ic ListCellData)
           (net.miginfocom.swing MigLayout)))

(set! *warn-on-reflection* true)
(set! *unchecked-math* true)

(declare save-tab-paint-settings)

(defn unit-selection-list-model ^DefaultListModel [units disable-reasons]  
  (let [^DefaultListModel model (DefaultListModel.)]
    (dotimes [i (count units)]
      (let [uval (nth units i)
            dr (nth disable-reasons i)
            unit-name (if (string? uval) uval (:name uval))
            unit (if (string? uval) (unit unit-name) uval)]
	      (.addElement model
	        (ic.ListCellData. 
	          (drawable-icon unit) 
	          (if dr
              dr
              unit-name) ; text
	          (if dr 
              nil 
              uval) ; return value
	          ))))
    model))
;    (.addElement (ic.ListCellData. (drawable-icon (ic.units/unit "Steam Tank")) "Some Text 2" nil))))


(defn ^JComponent unit-selection-list [units disable-reasons on-select]
  (let [jlist (JList.)
        scrollpane (JScrollPane. jlist)]
    (doto scrollpane
      (.setBorder (mikera.ui.steampunk.SteamPunkBorder. true))
      (.setHorizontalScrollBarPolicy JScrollPane/HORIZONTAL_SCROLLBAR_NEVER)
      (.setVerticalScrollBarPolicy JScrollPane/VERTICAL_SCROLLBAR_ALWAYS))
    (doto jlist
      (.setModel (unit-selection-list-model units disable-reasons))
      (.setCellRenderer (ic.ListCellRenderer.))
      (.setSelectionMode (ListSelectionModel/SINGLE_SELECTION))   
      (.addListSelectionListener 
        (proxy [ListSelectionListener] []
          (valueChanged [^ListSelectionEvent e]
            (let [^ListCellData lcd (.getSelectedValue ^JList jlist)
                  rval (.value lcd)]
              (if rval (on-select rval)))))))
    scrollpane))


(defn ^JPanel button-bar [buttons]
  (let [^JPanel panel (JPanel.)
        ^JPanel sub-panel (JPanel.)]
    (doto sub-panel
      (.setLayout (FlowLayout.))
      (.setBorder (EmptyBorder. 10 10 10 10)))
    (doto panel
      (.setLayout (BorderLayout.))
      (.add (JSeparator.) BorderLayout/NORTH)
      (.add sub-panel BorderLayout/CENTER))
    (doseq [^JButton button buttons]
      (.add sub-panel button))
    panel))

(defn ^JButton dialog-button [^String text]
  (doto (JButton. text)
    (.setPreferredSize (Dimension. 100 30))))

(defn ^JDialog select-unit-dialog [^String prompt units disable-reasons]
  (let [result (atom nil)
        ^JDialog dialog (JDialog. ic.frame/frame)
        cancel-button (dialog-button "Cancel")
        on-select (fn [value]
                    (swap! result (fn [res] value))
                    (.dispose dialog))
        ^JComponent unit-list (unit-selection-list units disable-reasons on-select )]
	  {:result result
	   :dialog 
			  (let []
	        (doto cancel-button 
	          (.addActionListener 
				      (proxy [ActionListener] []
				        (actionPerformed [^ActionEvent e]
				          (.dispose dialog)))))
	        (doto (.getContentPane dialog)
	          (.setLayout (BorderLayout.))
	          (.add (button-bar [cancel-button]) BorderLayout/SOUTH)
	          (.add unit-list BorderLayout/CENTER))
	        (doto dialog 
	          (.setLocationRelativeTo ic.frame/frame)
		        (.setTitle prompt)
		        (.setModal true)
	          (.pack))
	        dialog)}))


(defn show-select-unit-dialog 
  ([^String prompt units]
    (show-select-unit-dialog prompt units (map (fn [u] nil) units)))
  ([^String prompt units disable-reasons]
	  (let [dialog-data (select-unit-dialog prompt units disable-reasons)]
	    (.show ^JDialog (:dialog dialog-data))
	    @(:result dialog-data))))

; painting dialog

(def paint-side (atom (int 0)))
(def paint-unit (atom (ic.units/unit "Steam Tank")))

; atom to store reference to paint checkbox (set by ic.interface)
(def button-paint (atom nil))

(defn set-paint-mode! []
  (let [bp-checkbox ^JCheckBox @button-paint]
	  (if (not (.isSelected bp-checkbox))
	    (.doClick bp-checkbox))))

(defn unset-paint-mode! []
  (let [bp-checkbox ^JCheckBox @button-paint]
    (if (.isSelected bp-checkbox)
      (.doClick bp-checkbox))))

; paint functions should return an update to apply to the game

(defn paint-terrain-function [terrain params]
  (fn [game x y] 
    (let []
      (msg-set-terrain
        x
        y
        (merge 
          terrain 
          params)))))

(defn clear-terrain-function []
  (fn [game x y] 
    (let []
      (msg-set-terrain
        x
        y
        nil))))

(defn paint-unit-function [unit params]
  (fn [game x y] 
    (let [u (get-unit game x y)
          side @paint-side]
      (msg-add-unit
        (merge 
          unit 
          params
          {:player-id (:id (ic.game/get-player-for-side game side))
           :side side
           :dir (if (and u (:oriented-image u)) (mod (inc (:dir u)) 6) 0)}) 
        x 
        y))))

(defn clear-unit-function []
  (fn [game x y] 
    (let [u (get-unit game x y)]
        (if (nil? u)
          nil
          (msg-remove-unit u x y)))))

(defn paint-roads-function [type params]
  (fn [game x y] 
    (let [t (get-terrain game x y)]
;      (println "Painting: " type)
      (if (nil? t)
        nil
	      (msg-set-terrain
	        x
	        y
	        (merge 
	          t 
	          (if (= "Road" type)
	            {:has-road true}
	            {:has-rail true})))))))

(defn clear-roads-function []
  (fn [game x y] 
    (let [t (get-terrain game x y)]
      (if (nil? t)
        nil
	      (msg-set-terrain
	        x
	        y
	        (merge 
	          t 
	           {:has-road nil
	            :has-rail nil}))))))

(def paint-left-click 
  (atom 
    (paint-unit-function (ic.units/unit "Steam Tank") {})))

(def paint-right-click 
  (atom 
    (clear-unit-function)))


(def paint-nothing-function
  (fn [game x y] nil))


(defn paint-roads-list-model [] ^DefaultListModel 
  (let [^DefaultListModel model (DefaultListModel.)]
    (doseq [t ["Road" "Rail"]]
      (let []
        (.addElement model
          (ic.ListCellData. 
            nil 
            t ; text
            t ; return value
            ))))
    model))

(defn paint-units-list-model [side] ^DefaultListModel 
  (let [^DefaultListModel model (DefaultListModel.)]
    (doseq [unit ic.units/unit-types]
;      (println unit)
      (let [u (merge unit {:side side})]
	      (.addElement model
	        (ic.ListCellData. 
	          (drawable-icon u) 
	          (:name u) ; text
	          u ; return value
	          ))))
    model))

(defn paint-side-list-model [] ^DefaultListModel 
  (let [^DefaultListModel model (DefaultListModel.)]
    (dotimes [side 4]
      (.addElement model
        (ic.ListCellData. 
          (drawable-icon (ic.units/unit "Paddle Cruiser" {:side side})) 
          (["Green" "Red" "Blue" "Yellow"] side)  ; text
          side ; return value
          )))
    model))

(defn paint-terrain-list-model [] ^DefaultListModel 
  (let [^DefaultListModel model (DefaultListModel.)]
    (doseq [t ic.map/terrain-types]
      (.addElement model
        (ic.ListCellData. 
          (drawable-icon t) 
          (:name t) ; text
          t ; return value
          )))
    model))


(def unit-jlist (atom nil))

(defn ^JComponent paint-unit-list []
  (let [jlist (JList.)
        scrollpane (JScrollPane. jlist)]
    (doto scrollpane
      (.setBorder (mikera.ui.steampunk.SteamPunkBorder. true))
      (.setHorizontalScrollBarPolicy JScrollPane/HORIZONTAL_SCROLLBAR_NEVER)
      (.setVerticalScrollBarPolicy JScrollPane/VERTICAL_SCROLLBAR_ALWAYS))
    (doto jlist
      (.setModel (paint-units-list-model 0))
      (.setCellRenderer (ic.ListCellRenderer.))
      (.setLayoutOrientation JList/HORIZONTAL_WRAP)
      (.setSelectionMode (ListSelectionModel/SINGLE_SELECTION))   
      (.addListSelectionListener 
        (proxy [ListSelectionListener] []
          (valueChanged [^ListSelectionEvent e]
            (let [^ListCellData lcd (.getSelectedValue ^JList jlist)]
	            (if lcd
                (let [unit (.value lcd)]
	                (reset! paint-unit unit)
		              (swap! paint-left-click 
		                (fn [old] 
		                  (paint-unit-function unit {})))
                  (swap! paint-right-click 
                    (fn [old] 
                      (clear-unit-function)))))
              (save-tab-paint-settings "Units")
	            (set-paint-mode!))))))
    (reset! unit-jlist jlist)
    scrollpane))

(defn ^JComponent paint-side-list []
  (let [jlist (JList.)]
    (doto jlist
      (.setModel (paint-side-list-model))
      (.setCellRenderer (ic.ListCellRenderer.))
      (.setSelectionMode (ListSelectionModel/SINGLE_SELECTION))   
      (.addListSelectionListener 
        (proxy [ListSelectionListener] []
          (valueChanged [^ListSelectionEvent e]
            (let [^ListCellData lcd (.getSelectedValue ^JList jlist)
                  side (.value lcd)]
              (reset! paint-side side)
              (.setModel ^JList @unit-jlist (paint-units-list-model side))
              (set-paint-mode!))))))
     jlist))

(defn ^JComponent paint-terrain-list []
  (let [jlist (JList.)
        scrollpane (JScrollPane. jlist)]
    (doto scrollpane
      (.setBorder (mikera.ui.steampunk.SteamPunkBorder. true))
      (.setHorizontalScrollBarPolicy JScrollPane/HORIZONTAL_SCROLLBAR_NEVER)
      (.setVerticalScrollBarPolicy JScrollPane/VERTICAL_SCROLLBAR_ALWAYS))
    (doto jlist
      (.setModel (paint-terrain-list-model))
      (.setCellRenderer (ic.ListCellRenderer.))
      (.setLayoutOrientation JList/HORIZONTAL_WRAP)
      (.setSelectionMode (ListSelectionModel/SINGLE_SELECTION))   
      (.addListSelectionListener 
        (proxy [ListSelectionListener] []
          (valueChanged [^ListSelectionEvent e]
            (let [^ListCellData lcd (.getSelectedValue ^JList jlist)]
              (swap! paint-left-click 
                (fn [old] 
                  (paint-terrain-function (.value lcd) {})))
              (swap! paint-right-click 
                (fn [old] 
                  (clear-terrain-function)))
              (save-tab-paint-settings "Terrain")
              (set-paint-mode!))))))
    scrollpane))



(defn ^JComponent paint-roads-list []
  (let [jlist (JList.)
        scrollpane (JScrollPane. jlist)]
    (doto scrollpane
      (.setBorder (mikera.ui.steampunk.SteamPunkBorder. true))
      (.setHorizontalScrollBarPolicy JScrollPane/HORIZONTAL_SCROLLBAR_NEVER)
      (.setVerticalScrollBarPolicy JScrollPane/VERTICAL_SCROLLBAR_ALWAYS))
    (doto jlist
      (.setModel (paint-roads-list-model))
      (.setCellRenderer (ic.ListCellRenderer.))
      (.setLayoutOrientation JList/HORIZONTAL_WRAP)
      (.setSelectionMode (ListSelectionModel/SINGLE_SELECTION))   
      (.addListSelectionListener 
        (proxy [ListSelectionListener] []
          (valueChanged [^ListSelectionEvent e]
            (let [^ListCellData lcd (.getSelectedValue ^JList jlist)]
              (swap! paint-left-click 
                (fn [old] 
                  (paint-roads-function (.value lcd) {})))
              (swap! paint-right-click 
                (fn [old] 
                  (clear-roads-function)))
              (save-tab-paint-settings "Roads and Rail")
              (set-paint-mode!))))))
    scrollpane))

; functions to switch between tab painting functions

(def paint-tab-settings (atom {}))

(defn save-tab-paint-settings [tab]
  (let [prc @paint-right-click
        plc @paint-left-click]
    (swap! paint-tab-settings
      (fn [s]
        (assoc s tab {:left plc :right prc})))))

(defn re-select-tab [tab]
  (let [s (@paint-tab-settings tab)]
    (if (not (nil? s))
      (do 
        (swap! paint-left-click 
          (fn [old] 
            (:left s)))
        (swap! paint-right-click 
          (fn [old] 
            (:right s)))))))

; overall painting UI

(defn paint-unit-panel []
  (let [unit-list (paint-unit-list)
        side-list (paint-side-list)]
	  (doto (JPanel.)
	    (.setLayout (BorderLayout.))
	    (.add unit-list BorderLayout/CENTER)
	    (.add side-list BorderLayout/WEST))))

(def ^JDialog paint-dialog 
  (let [^JDialog dialog (JDialog. ic.frame/frame)
        tabbed-pane (JTabbedPane.)]
    (doto tabbed-pane
      (.addTab "Units" (paint-unit-panel))
      (.addTab "Terrain" (paint-terrain-list))
      (.addTab "Roads and Rail" (paint-roads-list))
      (.addChangeListener 
				(proxy [ChangeListener] []
	        (stateChanged [^ListSelectionEvent e]
	          (let [i (.getSelectedIndex tabbed-pane)
                  tab (.getTitleAt tabbed-pane i)]
              (re-select-tab tab))))))
    (doto (.getContentPane dialog)
      (.setLayout (BorderLayout.))
      (.add tabbed-pane))
    (doto dialog
      (.addWindowListener 
        (proxy [WindowAdapter] []
          (windowClosing [^WindowEvent e]
            (unset-paint-mode!))))
      (.pack))))

(defn show-paint-dialog []
   (.show paint-dialog))

