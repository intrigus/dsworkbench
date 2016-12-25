/* 
 * Copyright 2015 Torridity.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tor.tribes.ui.panels;

import de.tor.tribes.ui.views.DSWorkbenchAttackFrame;
import com.jidesoft.swing.JideTabbedPane;
import de.tor.tribes.types.Attack;
import de.tor.tribes.types.test.DummyUnit;
import de.tor.tribes.types.test.DummyVillage;
import de.tor.tribes.ui.components.CollapseExpandTrigger;
import de.tor.tribes.util.attack.AttackManager;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Date;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.UIResource;
import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.JXPanel;
import org.jdesktop.swingx.JXTaskPane;
import org.jdesktop.swingx.JXTaskPaneContainer;

/**
 *
 * @author Torridity
 */
public class GenericTestPanel extends javax.swing.JPanel {

    private JComponent centerComponent = null;
    private boolean menuEnabled = true;
    private JXTaskPaneContainer taskContainer = null;

    /** Creates new form GenericTestPanel */
    public GenericTestPanel(boolean menuEnabled) {
        this.menuEnabled = menuEnabled;
        initComponents();
        if (menuEnabled) {
            CollapseExpandTrigger trigger = new CollapseExpandTrigger();
            trigger.addMouseListener(new MouseAdapter() {

                @Override
                public void mouseReleased(MouseEvent e) {
                    menuPanel.setCollapsed(!menuPanel.isCollapsed());
                }
            });
            menuCollapsePanel.setBorder(BorderFactory.createLineBorder(Color.lightGray));
            menuCollapsePanel.add(trigger, BorderLayout.CENTER);
        } else {
            remove(menuPanel);
            centerPanel.remove(menuCollapsePanel);
        }

    }

    /** Creates new form GenericTestPanel */
    public GenericTestPanel() {
        this(true);
    }

    public void setMenuVisible(boolean pValue) {
        menuPanel.setCollapsed(!pValue);
    }

    public boolean isMenuVisible() {
        return !menuPanel.isCollapsed();
    }

    public void setupTaskPane(JComponent... pTaskPane) {
        taskContainer = new JXTaskPaneContainer();
        for (JComponent aPTaskPane : pTaskPane) {
            taskContainer.add(aPTaskPane);
        }
        menuPanel.remove(jXTaskPaneContainer1);
        JScrollPane s = new JScrollPane(taskContainer);
        s.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        menuPanel.add(s, BorderLayout.CENTER);
        taskContainer.setBackground(getBackground());
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        menuPanel = new org.jdesktop.swingx.JXCollapsiblePane();
        jXTaskPaneContainer1 = new org.jdesktop.swingx.JXTaskPaneContainer();
        centerPanel = new org.jdesktop.swingx.JXPanel();
        menuCollapsePanel = new org.jdesktop.swingx.JXPanel();

        setPreferredSize(new java.awt.Dimension(190, 100));
        setLayout(new java.awt.BorderLayout());

        menuPanel.setAnimated(false);
        menuPanel.setDirection(org.jdesktop.swingx.JXCollapsiblePane.Direction.RIGHT);
        menuPanel.setInheritAlpha(false);

        jXTaskPaneContainer1.setBackground(new java.awt.Color(240, 240, 240));
        jXTaskPaneContainer1.setMinimumSize(new java.awt.Dimension(170, 10));
        jXTaskPaneContainer1.setPreferredSize(new java.awt.Dimension(170, 10));
        menuPanel.add(jXTaskPaneContainer1, java.awt.BorderLayout.CENTER);

        add(menuPanel, java.awt.BorderLayout.EAST);

        centerPanel.setPreferredSize(new java.awt.Dimension(20, 20));
        centerPanel.setLayout(new java.awt.BorderLayout());

        menuCollapsePanel.setBackground(new java.awt.Color(204, 204, 204));
        menuCollapsePanel.setPreferredSize(new java.awt.Dimension(20, 473));
        menuCollapsePanel.setLayout(new java.awt.BorderLayout());
        centerPanel.add(menuCollapsePanel, java.awt.BorderLayout.EAST);

        add(centerPanel, java.awt.BorderLayout.CENTER);
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private org.jdesktop.swingx.JXPanel centerPanel;
    private org.jdesktop.swingx.JXTaskPaneContainer jXTaskPaneContainer1;
    private org.jdesktop.swingx.JXPanel menuCollapsePanel;
    private org.jdesktop.swingx.JXCollapsiblePane menuPanel;
    // End of variables declaration//GEN-END:variables

    /**
     * @return the childPanel
     */
    public JComponent getCenterComponent() {
        return centerComponent;
    }

    /**
     * @param childPanel the childPanel to set
     */
    public void setChildComponent(JComponent centerComponent) {
        this.centerComponent = centerComponent;
        centerPanel.removeAll();
        if (menuEnabled) {
            centerPanel.add(menuCollapsePanel, java.awt.BorderLayout.EAST);
        }
        if (centerComponent != null) {
            centerPanel.add(centerComponent, BorderLayout.CENTER);
        }
    }

    public static void main(String[] args) {
        try {
            //  UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception ignored) {
        }
        AttackManager.getSingleton().addGroup("test1");
        AttackManager.getSingleton().addGroup("asd2");
        AttackManager.getSingleton().addGroup("awe3");
        for (int i = 0; i < 100; i++) {
            Attack a = new Attack();
            a.setSource(new DummyVillage((short) (Math.random() * 100), (short) (Math.random() * 100)));
            a.setTarget(new DummyVillage((short) (Math.random() * 100), (short) (Math.random() * 100)));
            a.setArriveTime(new Date(Math.round(Math.random() * System.currentTimeMillis())));
            a.setUnit(new DummyUnit());
            AttackManager.getSingleton().addManagedElement(a);
            AttackManager.getSingleton().addManagedElement("test1", a);
            AttackManager.getSingleton().addManagedElement("asd2", a);
            AttackManager.getSingleton().addManagedElement("awe3", a);
        }
        JFrame f = new JFrame();
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setSize(300, 300);
        GenericTestPanel p = new GenericTestPanel();
        final JideTabbedPane t = new JideTabbedPane();
        t.setTabShape(JideTabbedPane.SHAPE_OFFICE2003);
        t.setTabColorProvider(JideTabbedPane.ONENOTE_COLOR_PROVIDER);
        t.setBoldActiveTab(true);
        /*   LabelUIResource2 lr = new LabelUIResource2();
        lr.setLayout(new BorderLayout());
        lr.add(jXPanel1, BorderLayout.CENTER);
        t.setTabLeadingComponent(lr);*/
        for (String group : AttackManager.getSingleton().getGroups()) {
            t.add(group, new AttackTableTab(group, null));
        }
        p.setChildComponent(t);
        t.getModel().addChangeListener(new ChangeListener() {

            @Override
            public void stateChanged(ChangeEvent e) {
                AttackTableTab tab = null;
                try {
                    if (t.getModel().getSelectedIndex() < 0) {
                        return;
                    }
                    tab = (AttackTableTab) t.getComponentAt(t.getModel().getSelectedIndex());
                } catch (ClassCastException cce) {
                    tab = null;
                }
                if (tab != null && !tab.getAttackPlan().equals("Neu")) {
                    tab.updatePlan();
                }
            }
        });
        // <editor-fold defaultstate="collapsed" desc="Edit task pane">
        JXTaskPane editTaskPane = new JXTaskPane();
        editTaskPane.setTitle("Editieren");
        editTaskPane.getContentPane().add(factoryButton("/res/ui/garbage.png", "Abgelaufene Angriffe entfernen", new MouseAdapter() {

            @Override
            public void mouseReleased(MouseEvent e) {
            }
        }));
        editTaskPane.getContentPane().add(factoryButton("/res/ui/att_changeTime.png", "Ankunftszeit für markierte Angriffe &auml;ndern. Die Startzeit der Angriffe wird dabei entsprechend der Laufzeit angepasst", new MouseAdapter() {

            @Override
            public void mouseReleased(MouseEvent e) {
            }
        }));
        editTaskPane.getContentPane().add(factoryButton("/res/ui/standard_attacks.png", "Einheit und Angriffstyp für markierte Angriffe &auml;ndern. Bitte beachte, dass sich beim &Auml;ndern der Einheit auch die Startzeit der Angriffe &auml;ndern kann", new MouseAdapter() {

            @Override
            public void mouseReleased(MouseEvent e) {
            }
        }));
        editTaskPane.getContentPane().add(factoryButton("/res/ui/att_browser_unsent.png", "'&Uuml;bertragen' Feld für markierte Angriffe l&ouml;schen", new MouseAdapter() {

            @Override
            public void mouseReleased(MouseEvent e) {
            }
        }));
        editTaskPane.getContentPane().add(factoryButton("/res/ui/pencil2.png", "Markierte Angriffe auf der Karte einzeichen. Ist ein gewählter Angriff bereits eingezeichnet, so wird er nach Bet&auml;tigung dieses Buttons nicht mehr eingezeichnet", new MouseAdapter() {

            @Override
            public void mouseReleased(MouseEvent e) {
            }
        }));
        // </editor-fold>
        // <editor-fold defaultstate="collapsed" desc="Transfer task pane">
        JXTaskPane transferTaskPane = new JXTaskPane();
        transferTaskPane.setTitle("Übertragen");
        transferTaskPane.getContentPane().add(factoryButton("/res/ui/att_clipboard.png", "Markierte Angriffe im Klartext in die Zwischenablage kopieren. Der Inhalt der Zwischenablage kann dann z.B. in Excel oder OpenOffice eingef&uuml;gt werden", new MouseAdapter() {

            @Override
            public void mouseReleased(MouseEvent e) {
            }
        }));
        transferTaskPane.getContentPane().add(factoryButton("/res/ui/att_clipboardBB.png", "Markierte Angriffe als BB-Codes in die Zwischenablage kopieren.Der Inhalt der Zwischenablage kann dann z.B. in das Stammesforum, die Notizen oder IGMs eingef&uuml;gt werden", new MouseAdapter() {

            @Override
            public void mouseReleased(MouseEvent e) {
            }
        }));

        transferTaskPane.getContentPane().add(factoryButton("/res/ui/att_HTML.png", "Markierte Angriffe in eine HTML Datei kopieren.<br/>Die erstellte Datei kann dann per eMail verschickt oder zum Abschicken von Angriffen ohne ge&ouml;ffnetesDS Workbench verwendet werden", new MouseAdapter() {

            @Override
            public void mouseReleased(MouseEvent e) {
            }
        }));
        transferTaskPane.getContentPane().add(factoryButton("/res/ui/atts_igm.png", "Markierte Angriffe als IGM verschicken. (PA notwendig) Der/die Empf&auml;nger der IGMs sind die Besitzer der Herkunftsd&ouml;rfer der geplanten Angriffe.", new MouseAdapter() {

            @Override
            public void mouseReleased(MouseEvent e) {
            }
        }));
        transferTaskPane.getContentPane().add(factoryButton("/res/ui/re-time.png", "Markierten Angriff in das Werkzeug 'Retimer' einfügen", new MouseAdapter() {

            @Override
            public void mouseReleased(MouseEvent e) {
            }
        }));
        transferTaskPane.getContentPane().add(factoryButton("/res/ui/att_browser.png", "Markierte Angriffe in den Browser &uuml;bertragen. Im Normalfall werden nur einzelne Angriffe &uuml;bertragen. F&uuml;r das &Uuml;bertragen mehrerer Angriff ist zuerst das Klickkonto entsprechend zu f&uuml;llen", new MouseAdapter() {

            @Override
            public void mouseReleased(MouseEvent e) {
            }
        }));
        transferTaskPane.getContentPane().add(factoryButton("/res/ui/export_js.png", "Markierte Angriffe in ein Userscript schreiben.Das erstellte Userscript muss im Anschluss manuell im Browser installiert werden. Als Ergebnis bekommt man an verschiedenen Stellen im Spiel Informationen &uuml;ber geplante Angriffe angezeigt.", new MouseAdapter() {

            @Override
            public void mouseReleased(MouseEvent e) {
            }
        }));
        // </editor-fold>
        // <editor-fold defaultstate="collapsed" desc="Misc task pane">

        JXTaskPane miscTaskPane = new JXTaskPane();
        miscTaskPane.setTitle("Sonstiges");
        miscTaskPane.getContentPane().add(factoryButton("/res/ui/standard_attacks.png", "Truppenst&auml;rke von Standardangriffen definieren. Diese Einstellungen werden verwendet, wenn man Angriffe in den Browser &uuml;bertr&auml;gt und das entsprechende Userscript 'dswb.user.js' installiert hat, um im ge&ouml;ffneten Versammlungsplatz Truppen bereits einzutragen", new MouseAdapter() {

            @Override
            public void mouseReleased(MouseEvent e) {
            }
        }));

        miscTaskPane.getContentPane().add(factoryButton("/res/ui/att_alert.png", "Aktiviert eine Warnung f&uuml;r Angriffe, welche in den n&auml;chsten 10 Minuten abgeschickt werden m&uuml;ssen", new MouseAdapter() {

            @Override
            public void mouseReleased(MouseEvent e) {
            }
        }));
        // </editor-fold>
        p.setupTaskPane(new JLabel("Test"), editTaskPane, transferTaskPane, miscTaskPane);
        f.add(p);
        f.pack();
        f.setVisible(true);
    }

    private static JXButton factoryButton(String pIconResource, String pTooltip, MouseListener pListener) {
        JXButton button = new JXButton(new ImageIcon(DSWorkbenchAttackFrame.class.getResource(pIconResource)));
        if (pTooltip != null) {
            button.setToolTipText("<html><div width='150px'>" + pTooltip + "</div></html>");
        }
        button.addMouseListener(pListener);
        return button;
    }
}

class LabelUIResource2 extends JXPanel implements UIResource {

    public LabelUIResource2() {
        super();
    }
}
