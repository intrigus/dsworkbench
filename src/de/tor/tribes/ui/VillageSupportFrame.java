/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * VillageSupportFrame.java
 *
 * Created on 30.12.2008, 14:02:10
 */
package de.tor.tribes.ui;

import de.tor.tribes.control.ManageableType;
import de.tor.tribes.io.DataHolder;
import de.tor.tribes.io.ServerManager;
import de.tor.tribes.io.UnitHolder;
import de.tor.tribes.types.Attack;
import de.tor.tribes.types.Barbarians;
import de.tor.tribes.types.Tag;
import de.tor.tribes.types.Village;
import de.tor.tribes.ui.renderer.DateCellRenderer;
import de.tor.tribes.ui.renderer.SortableTableHeaderRenderer;
import de.tor.tribes.util.Constants;
import de.tor.tribes.util.GlobalOptions;
import de.tor.tribes.util.SupportCalculator;
import de.tor.tribes.util.tag.TagManager;
import de.tor.tribes.util.troops.TroopsManager;
import de.tor.tribes.util.troops.VillageTroopsHolder;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import org.apache.log4j.Logger;
import de.tor.tribes.util.DSCalculator;
import de.tor.tribes.util.JOptionPaneHelper;
import de.tor.tribes.util.ServerSettings;
import de.tor.tribes.util.attack.AttackManager;
import java.awt.Color;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Iterator;
import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import org.jdesktop.swingx.painter.MattePainter;

/**
 * @author Charon
 */
public class VillageSupportFrame extends javax.swing.JFrame implements ActionListener {

    public static enum TRANSFER_TYPE {

        CLIPBOARD_BB, CUT_TO_INTERNAL_CLIPBOARD, COPY_TO_INTERNAL_CLIPBOARD
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        if (e.getActionCommand().equals("Copy")) {
            transferSelection(VillageSupportFrame.TRANSFER_TYPE.COPY_TO_INTERNAL_CLIPBOARD);
        } else if (e.getActionCommand().equals("BBCopy")) {
            transferSelection(VillageSupportFrame.TRANSFER_TYPE.CLIPBOARD_BB);
        } else if (e.getActionCommand().equals("Cut")) {
            transferSelection(VillageSupportFrame.TRANSFER_TYPE.CUT_TO_INTERNAL_CLIPBOARD);
        } else if (e.getActionCommand().equals("Delete")) {
            deleteSelection(true);
        }

    }
    private static Logger logger = Logger.getLogger("SupportDialog");
    private static VillageSupportFrame SINGLETON = null;
    private Village mCurrentVillage = null;

    public static synchronized VillageSupportFrame getSingleton() {
        if (SINGLETON == null) {
            SINGLETON = new VillageSupportFrame();
        }
        return SINGLETON;
    }

    /** Creates new form VillageSupportFrame */
    VillageSupportFrame() {
        initComponents();
        jTransferToAttackOverviewDialog.pack();

        KeyStroke copy = KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.CTRL_MASK, false);
        KeyStroke bbCopy = KeyStroke.getKeyStroke(KeyEvent.VK_B, ActionEvent.CTRL_MASK, false);
        KeyStroke cut = KeyStroke.getKeyStroke(KeyEvent.VK_X, ActionEvent.CTRL_MASK, false);
        KeyStroke delete = KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0, false);
        jSupportTable.registerKeyboardAction(this, "Copy", copy, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        jSupportTable.registerKeyboardAction(this, "Cut", cut, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        jSupportTable.registerKeyboardAction(this, "Delete", delete, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        jSupportTable.registerKeyboardAction(this, "BBCopy", bbCopy, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        jSupportTable.getActionMap().put("find", new AbstractAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                //ignore find
            }
        });

        // <editor-fold defaultstate="collapsed" desc=" Init HelpSystem ">
        GlobalOptions.getHelpBroker().enableHelpKey(getRootPane(), "pages.support_tool", GlobalOptions.getHelpBroker().getHelpSet());
        // </editor-fold>
    }

    public void transferSelection(TRANSFER_TYPE pType) {
        switch (pType) {
            case COPY_TO_INTERNAL_CLIPBOARD:
                copyToInternalClipboard();
                break;
            case CUT_TO_INTERNAL_CLIPBOARD:
                cutToInternalClipboard();
                break;
            case CLIPBOARD_BB:
                //@TODO move bb support to here
                break;
        }

    }

    private boolean copyToInternalClipboard() {
        List<Attack> selection = getSelectedSupports();
        if (selection.isEmpty()) {
            showInfo("Keine Unterstützungen gewählt");
            return false;
        }
        StringBuilder b = new StringBuilder();
        int cnt = 0;
        for (Attack a : selection) {
            b.append(Attack.toInternalRepresentation(a)).append("\n");
            cnt++;
        }
        try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(b.toString()), null);
            showSuccess(cnt + ((cnt == 1) ? " Unterstützung kopiert" : " Unterstützungen kopiert"));
            return true;
        } catch (HeadlessException hex) {
            showError("Fehler beim Kopieren der Unterstützungen");
            return false;
        }
    }

    private void cutToInternalClipboard() {
        int size = getSelectedSupports().size();
        if (size == 0) {
            showInfo("Keine Unterstützungen gewählt");
            return;
        }
        if (copyToInternalClipboard() && deleteSelection(false)) {
            showSuccess(size + ((size == 1) ? " Angriff ausgeschnitten" : " Angriffe ausgeschnitten"));
        } else {
            showError("Fehler beim Ausschneiden der Angriffe");
        }
    }

    public void showSuccess(String pMessage) {
        infoPanel.setCollapsed(false);
        jXLabel1.setBackgroundPainter(new MattePainter(Color.GREEN));
        jXLabel1.setForeground(Color.BLACK);
        jXLabel1.setText(pMessage);
    }

    public void showInfo(String pMessage) {
        infoPanel.setCollapsed(false);
        jXLabel1.setBackgroundPainter(new MattePainter(getBackground()));
        jXLabel1.setForeground(Color.BLACK);
        jXLabel1.setText(pMessage);
    }

    public void showError(String pMessage) {
        infoPanel.setCollapsed(false);
        jXLabel1.setBackgroundPainter(new MattePainter(Color.RED));
        jXLabel1.setForeground(Color.WHITE);
        jXLabel1.setText(pMessage);
    }

    public void showSupportFrame(Village pCurrent) {
        mCurrentVillage = pCurrent;
        setTitle("Unterstützung für " + mCurrentVillage);
        jScrollPane1.getViewport().setBackground(Constants.DS_BACK_LIGHT);
        DefaultListModel model = new DefaultListModel();
        for (ManageableType e : TagManager.getSingleton().getAllElements()) {
            Tag t = (Tag) e;
            model.addElement(t);
        }
        jTagsList.setModel(model);
        //select all
        jTagsList.getSelectionModel().setSelectionInterval(0, TagManager.getSingleton().getElementCount() - 1);
        jResultDialog.pack();
        setVisible(true);
    }

    public void showSupportFrame(Village pTarget, long pArriveTime) {
        mCurrentVillage = pTarget;
        setTitle("Unterstützung für " + mCurrentVillage);
        jScrollPane1.getViewport().setBackground(Constants.DS_BACK_LIGHT);
        DefaultListModel model = new DefaultListModel();
        for (ManageableType e : TagManager.getSingleton().getAllElements()) {
            Tag t = (Tag) e;
            model.addElement(t);
        }
        jTagsList.setModel(model);
        //select all
        jTagsList.getSelectionModel().setSelectionInterval(0, TagManager.getSingleton().getElementCount() - 1);
        SimpleDateFormat dateFormat = null;
        if (ServerSettings.getSingleton().isMillisArrival()) {
            dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss.SSS");
        } else {
            dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
        }
        dateTimeField.setDate(dateTimeField.getSelectedDate());
        jResultDialog.pack();
        setVisible(true);
    }

    private boolean deleteSelection(boolean pAsk) {
        List<Attack> selectedSupports = getSelectedSupports();
        if (pAsk) {
            String message = ((selectedSupports.size() == 1) ? "Unterstützung " : (selectedSupports.size() + " Unterstützungen ")) + "wirklich löschen?";
            if (selectedSupports.isEmpty() || JOptionPaneHelper.showQuestionConfirmBox(this, message, "Angriffe löschen", "Nein", "Ja") != JOptionPane.YES_OPTION) {
                return false;
            }
        }
        //@TODO implement delete

        return true;
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jResultDialog = new javax.swing.JDialog();
        jLabel5 = new javax.swing.JLabel();
        jTargetVillage = new javax.swing.JTextField();
        jButton3 = new javax.swing.JButton();
        jLabel6 = new javax.swing.JLabel();
        jArriveTime = new javax.swing.JTextField();
        jButton5 = new javax.swing.JButton();
        jButton6 = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        jSupportTable = new org.jdesktop.swingx.JXTable();
        infoPanel = new org.jdesktop.swingx.JXCollapsiblePane();
        jXLabel1 = new org.jdesktop.swingx.JXLabel();
        capabilityInfoPanel1 = new de.tor.tribes.ui.CapabilityInfoPanel();
        jTransferToAttackOverviewDialog = new javax.swing.JDialog();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jAttackPlansBox = new javax.swing.JComboBox();
        jNewPlanName = new javax.swing.JTextField();
        jButton9 = new javax.swing.JButton();
        jButton10 = new javax.swing.JButton();
        jButton7 = new javax.swing.JButton();
        jButton4 = new javax.swing.JButton();
        jButton8 = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jDefOnlyBox = new javax.swing.JCheckBox();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTagsList = new javax.swing.JList();
        jLabel4 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jMinUnitCountSpinner = new javax.swing.JSpinner();
        dateTimeField = new de.tor.tribes.ui.components.DateTimeField();

        jResultDialog.setTitle("Mögliche Unterstützungen");

        jLabel5.setText("Zu unterstützendes Dorf");
        jLabel5.setMaximumSize(new java.awt.Dimension(118, 25));
        jLabel5.setMinimumSize(new java.awt.Dimension(118, 25));
        jLabel5.setPreferredSize(new java.awt.Dimension(118, 25));

        jTargetVillage.setEditable(false);
        jTargetVillage.setMinimumSize(new java.awt.Dimension(6, 25));
        jTargetVillage.setPreferredSize(new java.awt.Dimension(6, 25));

        jButton3.setBackground(new java.awt.Color(239, 235, 223));
        jButton3.setText("Schließen");
        jButton3.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                fireCloseResultsEvent(evt);
            }
        });

        jLabel6.setText("Ankunftzeit");
        jLabel6.setMaximumSize(new java.awt.Dimension(55, 25));
        jLabel6.setMinimumSize(new java.awt.Dimension(55, 25));
        jLabel6.setPreferredSize(new java.awt.Dimension(55, 25));

        jArriveTime.setEditable(false);
        jArriveTime.setMinimumSize(new java.awt.Dimension(6, 25));
        jArriveTime.setPreferredSize(new java.awt.Dimension(6, 25));

        jButton5.setBackground(new java.awt.Color(239, 235, 223));
        jButton5.setIcon(new javax.swing.ImageIcon(getClass().getResource("/res/speed.png"))); // NOI18N
        jButton5.setToolTipText("Einordnung der Laufzeit für die in der Tabelle gewählte Einheit");
        jButton5.setMaximumSize(new java.awt.Dimension(57, 33));
        jButton5.setMinimumSize(new java.awt.Dimension(57, 33));
        jButton5.setPreferredSize(new java.awt.Dimension(57, 33));
        jButton5.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                fireShowTroopListEvent(evt);
            }
        });

        jButton6.setBackground(new java.awt.Color(239, 235, 223));
        jButton6.setIcon(new javax.swing.ImageIcon(getClass().getResource("/res/ally.png"))); // NOI18N
        jButton6.setToolTipText("Anzeige der maximalen Kampfkraft (Späher, Ramme und AG werden in jedem Fall ignoriert)");
        jButton6.setMaximumSize(new java.awt.Dimension(57, 33));
        jButton6.setMinimumSize(new java.awt.Dimension(57, 33));
        jButton6.setPreferredSize(new java.awt.Dimension(57, 33));
        jButton6.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                fireCalculateForceEvent(evt);
            }
        });

        jPanel1.setLayout(new java.awt.BorderLayout());

        jSupportTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane3.setViewportView(jSupportTable);

        jPanel1.add(jScrollPane3, java.awt.BorderLayout.CENTER);

        infoPanel.setCollapsed(true);
        infoPanel.setInheritAlpha(false);

        jXLabel1.setOpaque(true);
        jXLabel1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                jXLabel1fireHideInfoEvent(evt);
            }
        });
        infoPanel.add(jXLabel1, java.awt.BorderLayout.CENTER);

        jPanel1.add(infoPanel, java.awt.BorderLayout.SOUTH);

        capabilityInfoPanel1.setPastable(false);
        capabilityInfoPanel1.setSearchable(false);

        javax.swing.GroupLayout jResultDialogLayout = new javax.swing.GroupLayout(jResultDialog.getContentPane());
        jResultDialog.getContentPane().setLayout(jResultDialogLayout);
        jResultDialogLayout.setHorizontalGroup(
            jResultDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jResultDialogLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jResultDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 583, Short.MAX_VALUE)
                    .addGroup(jResultDialogLayout.createSequentialGroup()
                        .addGroup(jResultDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jLabel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jLabel5, javax.swing.GroupLayout.DEFAULT_SIZE, 139, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jResultDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jTargetVillage, javax.swing.GroupLayout.DEFAULT_SIZE, 434, Short.MAX_VALUE)
                            .addComponent(jArriveTime, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 434, Short.MAX_VALUE)))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jResultDialogLayout.createSequentialGroup()
                        .addComponent(capabilityInfoPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 458, Short.MAX_VALUE)
                        .addComponent(jButton3))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jResultDialogLayout.createSequentialGroup()
                        .addComponent(jButton6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        jResultDialogLayout.setVerticalGroup(
            jResultDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jResultDialogLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jResultDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTargetVillage, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jResultDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jArriveTime, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jResultDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButton5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButton6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(13, 13, 13)
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 344, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addGroup(jResultDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(capabilityInfoPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jButton3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        jTransferToAttackOverviewDialog.setTitle("In Angriffsplan einfügen");
        jTransferToAttackOverviewDialog.setAlwaysOnTop(true);

        jLabel7.setText("Existierender Plan");

        jLabel8.setText("Neuer Plan");

        jButton9.setText("Einfügen");
        jButton9.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                fireTransferSupportsEvent(evt);
            }
        });

        jButton10.setText("Abbrechen");
        jButton10.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                fireCancelTransferSupportsEvent(evt);
            }
        });

        javax.swing.GroupLayout jTransferToAttackOverviewDialogLayout = new javax.swing.GroupLayout(jTransferToAttackOverviewDialog.getContentPane());
        jTransferToAttackOverviewDialog.getContentPane().setLayout(jTransferToAttackOverviewDialogLayout);
        jTransferToAttackOverviewDialogLayout.setHorizontalGroup(
            jTransferToAttackOverviewDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jTransferToAttackOverviewDialogLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jTransferToAttackOverviewDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jTransferToAttackOverviewDialogLayout.createSequentialGroup()
                        .addGroup(jTransferToAttackOverviewDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel7)
                            .addComponent(jLabel8))
                        .addGap(18, 18, 18)
                        .addGroup(jTransferToAttackOverviewDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jNewPlanName, javax.swing.GroupLayout.DEFAULT_SIZE, 266, Short.MAX_VALUE)
                            .addComponent(jAttackPlansBox, javax.swing.GroupLayout.Alignment.LEADING, 0, 266, Short.MAX_VALUE)))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jTransferToAttackOverviewDialogLayout.createSequentialGroup()
                        .addComponent(jButton10)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton9)))
                .addContainerGap())
        );
        jTransferToAttackOverviewDialogLayout.setVerticalGroup(
            jTransferToAttackOverviewDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jTransferToAttackOverviewDialogLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jTransferToAttackOverviewDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(jAttackPlansBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jTransferToAttackOverviewDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jNewPlanName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel8))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jTransferToAttackOverviewDialogLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton9)
                    .addComponent(jButton10))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jButton7.setBackground(new java.awt.Color(239, 235, 223));
        jButton7.setIcon(new javax.swing.ImageIcon(getClass().getResource("/res/ui/att_clipboardBB.png"))); // NOI18N
        jButton7.setToolTipText("Als BB-Code in die Zwischenablage kopieren");
        jButton7.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                fireCopyBBCodeToClipboardEvent(evt);
            }
        });

        jButton4.setBackground(new java.awt.Color(239, 235, 223));
        jButton4.setIcon(new javax.swing.ImageIcon(getClass().getResource("/res/ui/att_clipboard.png"))); // NOI18N
        jButton4.setToolTipText("Unformatiert in die Zwischenablage kopieren");
        jButton4.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                fireCopyUnformatedToClipboardEvent(evt);
            }
        });

        jButton8.setBackground(new java.awt.Color(239, 235, 223));
        jButton8.setIcon(new javax.swing.ImageIcon(getClass().getResource("/res/ui/att_overview.png"))); // NOI18N
        jButton8.setToolTipText("Truppenbewegungen in Angriffsübersicht einfügen");
        jButton8.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                fireMoveSupportsToAttackViewEvent(evt);
            }
        });

        setTitle("Unterstützung");

        jLabel1.setText("Ankunftzeit");
        jLabel1.setMaximumSize(new java.awt.Dimension(150, 25));
        jLabel1.setMinimumSize(new java.awt.Dimension(150, 25));
        jLabel1.setPreferredSize(new java.awt.Dimension(150, 25));

        jDefOnlyBox.setSelected(true);
        jDefOnlyBox.setToolTipText("Bei der Berechnung nur echte Deff-Einheiten (Speer, Schwert, Bogen, SKav) berücksichtigen. Rammen, Späher und AGs werden in jedem Fall ignoriert.");
        jDefOnlyBox.setAlignmentY(0.0F);
        jDefOnlyBox.setIconTextGap(0);
        jDefOnlyBox.setMargin(new java.awt.Insets(0, 0, 0, 0));
        jDefOnlyBox.setMaximumSize(new java.awt.Dimension(17, 25));
        jDefOnlyBox.setMinimumSize(new java.awt.Dimension(17, 25));
        jDefOnlyBox.setOpaque(false);
        jDefOnlyBox.setPreferredSize(new java.awt.Dimension(17, 25));

        jLabel2.setText("Nur Deff berücksichtigen");
        jLabel2.setMaximumSize(new java.awt.Dimension(150, 25));
        jLabel2.setMinimumSize(new java.awt.Dimension(150, 25));
        jLabel2.setPreferredSize(new java.awt.Dimension(150, 25));

        jLabel3.setText("<html>Dörfer aus folgenden Gruppen berücksichtigen</html>");
        jLabel3.setMaximumSize(new java.awt.Dimension(150, 100));
        jLabel3.setMinimumSize(new java.awt.Dimension(150, 14));
        jLabel3.setPreferredSize(new java.awt.Dimension(150, 30));

        jScrollPane1.setViewportView(jTagsList);

        jLabel4.setText("Min. Anzahl Einheiten");
        jLabel4.setMaximumSize(new java.awt.Dimension(150, 25));
        jLabel4.setMinimumSize(new java.awt.Dimension(150, 25));
        jLabel4.setPreferredSize(new java.awt.Dimension(150, 25));

        jButton1.setText("Berechnen");
        jButton1.setToolTipText("Starte die Berechnung der maximalen Kampfkraft");
        jButton1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                fireCalculateEvent(evt);
            }
        });

        jButton2.setText("Schließen");
        jButton2.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                fireCancelEvent(evt);
            }
        });

        jMinUnitCountSpinner.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(0), Integer.valueOf(0), null, Integer.valueOf(1)));
        jMinUnitCountSpinner.setToolTipText("Minimale Anzahl der Einheiten aus einem Dorf, die als Unterstützung berücksichtigt werden");
        jMinUnitCountSpinner.setEditor(new javax.swing.JSpinner.NumberEditor(jMinUnitCountSpinner, ""));
        jMinUnitCountSpinner.setMinimumSize(new java.awt.Dimension(31, 25));
        jMinUnitCountSpinner.setPreferredSize(new java.awt.Dimension(31, 25));

        dateTimeField.setToolTipText("Datum und Uhrzeit des Zeitrahmens");
        dateTimeField.setEnabled(false);
        dateTimeField.setMaximumSize(new java.awt.Dimension(32767, 25));
        dateTimeField.setMinimumSize(new java.awt.Dimension(64, 25));
        dateTimeField.setPreferredSize(new java.awt.Dimension(258, 25));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel4, 0, 0, Short.MAX_VALUE)
                    .addComponent(jLabel1, 0, 0, Short.MAX_VALUE)
                    .addComponent(jLabel2, 0, 0, Short.MAX_VALUE)
                    .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 144, Short.MAX_VALUE))
                .addGap(16, 16, 16)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jDefOnlyBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jMinUnitCountSpinner, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 255, Short.MAX_VALUE)
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 255, Short.MAX_VALUE)
                            .addComponent(dateTimeField, javax.swing.GroupLayout.DEFAULT_SIZE, 255, Short.MAX_VALUE)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jButton2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButton1)))
                        .addGap(21, 21, 21)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(dateTimeField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jDefOnlyBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, 203, Short.MAX_VALUE)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 203, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jMinUnitCountSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton1)
                    .addComponent(jButton2))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void fireCancelEvent(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fireCancelEvent
        setVisible(false);
    }//GEN-LAST:event_fireCancelEvent

    private void fireCalculateEvent(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fireCalculateEvent
        boolean defOnly = jDefOnlyBox.isSelected();
        Date arrive = dateTimeField.getSelectedDate();
        Integer minUnitCnt = (Integer) jMinUnitCountSpinner.getValue();
        List<Tag> allowedTags = new LinkedList<Tag>();
        for (Object o : jTagsList.getSelectedValues()) {
            allowedTags.add((Tag) o);
        }

        List<SupportCalculator.SupportMovement> movements = SupportCalculator.calculateSupport(mCurrentVillage, arrive, defOnly, allowedTags, minUnitCnt);
        if ((movements == null) || (movements.size() == 0)) {
            JOptionPaneHelper.showWarningBox(this, "Mit den eingestellten Parametern ist keine Unterstützung möglich.", "Warnung");
            return;
        } else {
            buildResults(movements);
            jTargetVillage.setText(mCurrentVillage.toString());
            jArriveTime.setText(new SimpleDateFormat("dd.MM.yy HH:mm:ss").format(dateTimeField.getSelectedDate()));
            jResultDialog.setLocationRelativeTo(this);
            jResultDialog.setVisible(true);
        }
    }//GEN-LAST:event_fireCalculateEvent

    private void fireShowTroopListEvent(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fireShowTroopListEvent
        UnitHolder selectedUnit = null;
        int row = jSupportTable.getSelectedRow();
        if (row >= 0) {
            //row = jSupportTable.convertRowIndexToModel(row);
            selectedUnit = (UnitHolder) jSupportTable.getValueAt(row, 1);
        }
        UnitOrderBuilder.showUnitOrder(null, selectedUnit);
    }//GEN-LAST:event_fireShowTroopListEvent

    private void fireCloseResultsEvent(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fireCloseResultsEvent
        jResultDialog.setVisible(false);
    }//GEN-LAST:event_fireCloseResultsEvent

    private void fireCalculateForceEvent(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fireCalculateForceEvent
        calculateForce();
    }//GEN-LAST:event_fireCalculateForceEvent

    private void fireCopyUnformatedToClipboardEvent(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fireCopyUnformatedToClipboardEvent
        try {
            int[] rows = jSupportTable.getSelectedRows();
            if ((rows != null) && (rows.length > 0)) {
                StringBuffer buffer = new StringBuffer();
                for (int i : rows) {
                    int row = i;//jSupportTable.convertRowIndexToModel(i);
                    Village source = (Village) jSupportTable.getValueAt(row, 0);
                    UnitHolder sUnit = (UnitHolder) jSupportTable.getValueAt(row, 1);
                    Date sTime = (Date) jSupportTable.getValueAt(row, 2);
                    String sendtime = null;
                    if (ServerSettings.getSingleton().isMillisArrival()) {
                        sendtime = new SimpleDateFormat("dd.MM.yy HH:mm:ss.SSS").format(sTime);
                    } else {
                        sendtime = new SimpleDateFormat("dd.MM.yy HH:mm:ss").format(sTime);
                    }
                    String v = jTargetVillage.getText();
                    String coord = v.substring(v.lastIndexOf("(") + 1, v.lastIndexOf(")")).trim();
                    String[] pos = coord.split("\\|");
                    int x = Integer.parseInt(pos[0]);
                    int y = Integer.parseInt(pos[1]);
                    Village target = DataHolder.getSingleton().getVillages()[x][y];

                    if (source.getTribe() == Barbarians.getSingleton()) {
                        buffer.append("Barbaren");
                    } else {
                        buffer.append(source.getTribe());
                    }
                    buffer.append("\t");
                    buffer.append(source);
                    buffer.append("\t");
                    if (target.getTribe() == Barbarians.getSingleton()) {
                        buffer.append("Barbaren");
                    } else {
                        buffer.append(target.getTribe());
                    }
                    buffer.append("\t");
                    buffer.append(target);
                    buffer.append("\t");
                    buffer.append(sUnit);
                    buffer.append("\t");
                    buffer.append(sendtime);
                    buffer.append("\t");
                    buffer.append(jArriveTime.getText());
                    buffer.append("\n");
                }

                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(buffer.toString()), null);
                String result = "Daten in Zwischenablage kopiert.";
                JOptionPaneHelper.showInformationBox(jResultDialog, result, "Information");
            } else {
                JOptionPaneHelper.showWarningBox(jResultDialog, "Keine Unterstützungen ausgewählt.", "Warnung");
            }
        } catch (Exception e) {
            logger.error("Failed to copy data to clipboard", e);
            String result = "Fehler beim Kopieren in die Zwischenablage.";
            JOptionPaneHelper.showErrorBox(jResultDialog, result, "Fehler");
        }
    }//GEN-LAST:event_fireCopyUnformatedToClipboardEvent

    private void fireCopyBBCodeToClipboardEvent(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fireCopyBBCodeToClipboardEvent
        try {
            int[] rows = jSupportTable.getSelectedRows();
            if ((rows != null) && (rows.length > 0)) {
                boolean extended = (JOptionPaneHelper.showQuestionConfirmBox(jResultDialog, "Erweiterte BB-Codes verwenden (nur für Forum und Notizen geeignet)?", "Erweiterter BB-Code", "Nein", "Ja") == JOptionPane.YES_OPTION);

                StringBuffer buffer = new StringBuffer();
                if (extended) {
                    buffer.append("[u][size=12]Unterstützungsplan[/size][/u]\n\n");
                } else {
                    buffer.append("[u]Unterstützungsplan[/u]\n\n");
                }
                String v = jTargetVillage.getText();
                String coord = v.substring(v.lastIndexOf("(") + 1, v.lastIndexOf(")")).trim();
                String[] pos = coord.split("\\|");
                int x = Integer.parseInt(pos[0]);
                int y = Integer.parseInt(pos[1]);
                Village target = DataHolder.getSingleton().getVillages()[x][y];
                buffer.append("[quote]Zu unterstützendes Dorf: " + target.toBBCode() + "\n");
                Date arrive = new SimpleDateFormat("dd.MM.yy HH:mm:ss").parse(jArriveTime.getText());
                buffer.append("Geplante Ankunft: ");
                if (extended) {
                    if (ServerSettings.getSingleton().isMillisArrival()) {
                        buffer.append(new SimpleDateFormat("'[color=green]'dd.MM.yy 'um' HH:mm:ss.'[size=8]'SSS'[/size][/color]'").format(arrive) + "[/quote]\n\n");
                    } else {
                        buffer.append(new SimpleDateFormat("'[color=green]'dd.MM.yy 'um' HH:mm:ss'[/color]'").format(arrive) + "[/quote]\n\n");
                    }
                } else {
                    if (ServerSettings.getSingleton().isMillisArrival()) {
                        buffer.append(new SimpleDateFormat("'[color=green]'dd.MM.yy 'um' HH:mm:ss.SSS'[/color]'").format(arrive) + "[/quote]\n\n");
                    } else {
                        buffer.append(new SimpleDateFormat("'[color=green]'dd.MM.yy 'um' HH:mm:ss'[/color]'").format(arrive) + "[/quote]\n\n");
                    }
                }

                String sUrl = ServerManager.getServerURL(GlobalOptions.getSelectedServer());
                for (int i : rows) {
                    int row = i;//jSupportTable.convertRowIndexToModel(i);
                    Village source = (Village) jSupportTable.getValueAt(row, 0);
                    UnitHolder unit = (UnitHolder) jSupportTable.getValueAt(row, 1);
                    Date sendTime = (Date) jSupportTable.getValueAt(row, 2);
                    buffer.append("");
                    String sendtime = "";
                    if (extended) {
                        if (ServerSettings.getSingleton().isMillisArrival()) {
                            sendtime = new SimpleDateFormat("'[color=red]'dd.MM.yy 'um' HH:mm:ss.'[size=8]'SSS'[/size][/color]'").format(sendTime);
                        } else {
                            sendtime = new SimpleDateFormat("'[color=red]'dd.MM.yy 'um' HH:mm:ss'[/color]'").format(sendTime);
                        }
                    } else {
                        if (ServerSettings.getSingleton().isMillisArrival()) {
                            sendtime = new SimpleDateFormat("'[color=red]'dd.MM.yy 'um' HH:mm:ss.SSS'[/color]'").format(sendTime);
                        } else {
                            sendtime = new SimpleDateFormat("'[color=red]'dd.MM.yy 'um' HH:mm:ss'[/color]'").format(sendTime);
                        }
                    }
                    buffer.append("Unterstützung aus ");
                    buffer.append(source.toBBCode());
                    buffer.append(" mit ");
                    if (extended) {
                        buffer.append("[img]" + sUrl + "/graphic/unit/unit_" + unit.getPlainName() + ".png[/img]");
                    } else {
                        buffer.append(unit.getName());
                    }
                    buffer.append(" startet am ");
                    buffer.append(sendtime);
                    buffer.append("\n");
                }
                if (extended) {
                    buffer.append("\n[size=8]Erstellt am ");
                    buffer.append(new SimpleDateFormat("dd.MM.yy 'um' HH:mm:ss").format(Calendar.getInstance().getTime()));
                    buffer.append(" mit [url=\"http://www.dsworkbench.de/index.php?id=23\"]DS Workbench ");
                    buffer.append(Constants.VERSION + Constants.VERSION_ADDITION + "[/url][/size]\n");
                } else {
                    buffer.append("\nErstellt am ");
                    buffer.append(new SimpleDateFormat("dd.MM.yy 'um' HH:mm:ss").format(Calendar.getInstance().getTime()));
                    buffer.append(" mit [url=\"http://www.dsworkbench.de/index.php?id=23\"]DS Workbench ");
                    buffer.append(Constants.VERSION + Constants.VERSION_ADDITION + "[/url]\n");
                }

                String b = buffer.toString();
                StringTokenizer t = new StringTokenizer(b, "[");
                int cnt = t.countTokens();
                if (cnt > 1000) {
                    if (JOptionPaneHelper.showQuestionConfirmBox(jResultDialog, "Die zu exportierenden Unterstützungen benötigen mehr als 1000 BB-Codes\n" + "und können daher im Spiel (Forum/IGM/Notizen) nicht auf einmal dargestellt werden.\nTrotzdem exportieren?", "Zu viele BB-Codes", "Nein", "Ja") == JOptionPane.NO_OPTION) {
                        return;
                    }
                }
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(b), null);
                String result = "Daten in Zwischenablage kopiert.";
                JOptionPaneHelper.showInformationBox(jResultDialog, result, "Information");
            } else {
                JOptionPaneHelper.showWarningBox(jResultDialog, "Keine Unterstützungen ausgewählt.", "Warnung");
            }
        } catch (Exception e) {
            logger.error("Failed to copy data to clipboard", e);
            String result = "Fehler beim Kopieren in die Zwischenablage.";
            JOptionPaneHelper.showErrorBox(jResultDialog, result, "Fehler");
        }
    }//GEN-LAST:event_fireCopyBBCodeToClipboardEvent

    private void fireMoveSupportsToAttackViewEvent(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fireMoveSupportsToAttackViewEvent
        int[] rows = jSupportTable.getSelectedRows();
        if (rows == null || rows.length == 0) {
            JOptionPaneHelper.showErrorBox(jResultDialog, "Keine Unterstützungen ausgewählt", "Fehler");
            return;
        }
        jNewPlanName.setText("");
        Iterator<String> plans = AttackManager.getSingleton().getGroupIterator();
        DefaultComboBoxModel model = new DefaultComboBoxModel();
        while (plans.hasNext()) {
            model.addElement(plans.next());
        }
        jAttackPlansBox.setModel(model);
        jTransferToAttackOverviewDialog.setLocationRelativeTo(jResultDialog);
        jTransferToAttackOverviewDialog.setVisible(true);
    }//GEN-LAST:event_fireMoveSupportsToAttackViewEvent

    private void fireTransferSupportsEvent(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fireTransferSupportsEvent
        String planName = jNewPlanName.getText();
        if (planName.length() < 1) {
            int idx = jAttackPlansBox.getSelectedIndex();
            if (idx < 0) {
                planName = null;
            } else {
                planName = (String) jAttackPlansBox.getSelectedItem();
            }
        }
        AttackManager.getSingleton().addGroup(planName);


        if (logger.isDebugEnabled()) {
            logger.debug("Adding attacks to plan '" + planName + "'");
        }

        String v = jTargetVillage.getText();
        String coord = v.substring(v.lastIndexOf("(") + 1, v.lastIndexOf(")")).trim();
        int[] co = null;
        if (ServerSettings.getSingleton().getCoordType() != 2) {
            String[] pos = coord.split("\\:");
            int x = Integer.parseInt(pos[0]);
            int y = Integer.parseInt(pos[1]);
            int z = Integer.parseInt(pos[2]);
            co = DSCalculator.hierarchicalToXy(x, y, z);
        } else {
            String[] pos = coord.split("\\|");
            int x = Integer.parseInt(pos[0]);
            int y = Integer.parseInt(pos[1]);
            co = new int[]{x, y};
        }
        int[] rows = jSupportTable.getSelectedRows();
        Village target = DataHolder.getSingleton().getVillages()[co[0]][co[1]];
        DefaultTableModel resultModel = (DefaultTableModel) jSupportTable.getModel();
        AttackManager.getSingleton().invalidate();
        for (int r : rows) {
            int row = jSupportTable.convertRowIndexToModel(r);
            Village source = (Village) resultModel.getValueAt(row, 0);
            UnitHolder unit = (UnitHolder) resultModel.getValueAt(row, 1);
            Date sendTime = (Date) resultModel.getValueAt(row, 2);
            long arriveTime = sendTime.getTime() + (long) (DSCalculator.calculateMoveTimeInSeconds(source, target, unit.getSpeed()) * 1000);
            AttackManager.getSingleton().addAttack(source, target, unit, new Date(arriveTime), false, planName, Attack.SUPPORT_TYPE, false);
        }
        AttackManager.getSingleton().revalidate();
        jTransferToAttackOverviewDialog.setVisible(false);
    }//GEN-LAST:event_fireTransferSupportsEvent

    private void fireCancelTransferSupportsEvent(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fireCancelTransferSupportsEvent
        jTransferToAttackOverviewDialog.setVisible(false);
    }//GEN-LAST:event_fireCancelTransferSupportsEvent

    private void jXLabel1fireHideInfoEvent(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jXLabel1fireHideInfoEvent
        infoPanel.setCollapsed(true);
}//GEN-LAST:event_jXLabel1fireHideInfoEvent

    private void buildResults(List<SupportCalculator.SupportMovement> pMovements) {
        DefaultTableModel model = new javax.swing.table.DefaultTableModel(
                new Object[][]{},
                new String[]{
                    "Herkunftsdorf", "Truppen", "Abschickzeit", "#Verwendungen"
                }) {

            Class[] types = new Class[]{
                Village.class, UnitHolder.class, Date.class, Integer.class
            };

            @Override
            public Class getColumnClass(int columnIndex) {
                return types[columnIndex];
            }

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return false;
            }
        };

        jSupportTable.setModel(model);
        jSupportTable.setRowSorter(new TableRowSorter(model));
        DefaultTableCellRenderer headerRenderer = new SortableTableHeaderRenderer();
        for (int i = 0; i < jSupportTable.getColumnCount(); i++) {
            jSupportTable.getColumn(jSupportTable.getColumnName(i)).setHeaderRenderer(headerRenderer);
        }

        if (ServerSettings.getSingleton().isMillisArrival()) {
            jSupportTable.setDefaultRenderer(Date.class, new DateCellRenderer("dd.MM.yy HH:mm:ss.SSS"));
        } else {
            jSupportTable.setDefaultRenderer(Date.class, new DateCellRenderer("dd.MM.yy HH:mm:ss"));
        }
        for (SupportCalculator.SupportMovement movement : pMovements) {
            Village village = movement.getSource();
            UnitHolder unit = movement.getUnit();
            Date sendTime = movement.getSendTime();
            int usages = 0;
            Iterator<String> plans = AttackManager.getSingleton().getGroupIterator();
            while (plans.hasNext()) {
                String planId = plans.next();
                List<ManageableType> plan = AttackManager.getSingleton().getAllElements(planId);
                for (ManageableType e : plan) {
                    Attack a = (Attack) e;
                    if (a.getSource().equals(village) && a.getType() == Attack.SUPPORT_TYPE) {
                        usages++;
                    }
                }
            }
            model.addRow(new Object[]{village, unit, sendTime, usages});
        }
    }

    private void calculateForce() {
        UnitHolder[] units = DataHolder.getSingleton().getUnits().toArray(new UnitHolder[]{});
        //sort units descending
        Arrays.sort(units, new Comparator<UnitHolder>() {

            @Override
            public int compare(UnitHolder o1, UnitHolder o2) {
                if (o1.getSpeed() == o2.getSpeed()) {
                    return 0;
                } else if (o1.getSpeed() < o2.getSpeed()) {
                    return 1;
                } else {
                    return -1;
                }
            }
        });

        Hashtable<UnitHolder, Integer> forceTable = new Hashtable<UnitHolder, Integer>();
        for (int i = 0; i < jSupportTable.getRowCount(); i++) {
            int row = i;//jSupportTable.convertRowIndexToModel(i);
            Village v = (Village) jSupportTable.getValueAt(row, 0);
            UnitHolder u = (UnitHolder) jSupportTable.getValueAt(row, 1);
            VillageTroopsHolder troops = TroopsManager.getSingleton().getTroopsForVillage(v, TroopsManager.TROOP_TYPE.OWN);
            boolean useUnits = false;
            for (int j = 0; j < units.length; j++) {
                if (!useUnits) {
                    //if no unit is used yet
                    if (units[j].equals(u)) {
                        //use all following units
                        useUnits = true;
                    }
                }

                if (useUnits) {
                    if (jDefOnlyBox.isSelected()) {
                        if (units[j].getPlainName().equals("spear") || units[j].getPlainName().equals("sword") || units[j].getPlainName().equals("archer") || units[j].getPlainName().equals("heavy")) {

                            if (troops != null) {
                                int cnt = troops.getTroopsOfUnitInVillage(units[j]);
                                if (forceTable.get(units[j]) != null) {
                                    forceTable.put(units[j], forceTable.get(units[j]) + cnt);
                                } else {
                                    forceTable.put(units[j], cnt);
                                }
                            }
                        }
                    } else {
                        if (!units[j].getPlainName().equals("spy") && !units[j].getPlainName().equals("ram") && !units[j].getPlainName().equals("snob")) {
                            if (troops != null) {
                                int cnt = troops.getTroopsOfUnitInVillage(units[j]);
                                if (forceTable.get(units[j]) != null) {
                                    forceTable.put(units[j], forceTable.get(units[j]) + cnt);
                                } else {
                                    forceTable.put(units[j], cnt);
                                }
                            }
                        }
                    }
                }
            }
        }

        //add units of current village
        for (int j = 0; j < units.length; j++) {
            if (jDefOnlyBox.isSelected()) {
                if (units[j].getPlainName().equals("spear") || units[j].getPlainName().equals("sword") || units[j].getPlainName().equals("archer") || units[j].getPlainName().equals("heavy")) {
                    VillageTroopsHolder troops = TroopsManager.getSingleton().getTroopsForVillage(mCurrentVillage);
                    if (troops != null) {
                        int cnt = troops.getTroopsOfUnitInVillage(units[j]);
                        if (forceTable.get(units[j]) != null) {
                            forceTable.put(units[j], forceTable.get(units[j]) + cnt);
                        } else {
                            forceTable.put(units[j], cnt);
                        }
                    }
                }
            } else {
                if (!units[j].getPlainName().equals("spy") && !units[j].getPlainName().equals("ram") && !units[j].getPlainName().equals("snob")) {
                    VillageTroopsHolder troops = TroopsManager.getSingleton().getTroopsForVillage(mCurrentVillage);
                    if (troops != null) {
                        int cnt = troops.getTroopsOfUnitInVillage(units[j]);
                        if (forceTable.get(units[j]) != null) {
                            forceTable.put(units[j], forceTable.get(units[j]) + cnt);
                        } else {
                            forceTable.put(units[j], cnt);
                        }
                    }
                }
            }
        }

        StringBuffer buffer = new StringBuffer();
        buffer.append("Die maximale Kampfkraft beträgt:\n");
        NumberFormat nf = NumberFormat.getInstance();
        nf.setMaximumFractionDigits(0);
        nf.setMinimumFractionDigits(0);
        for (UnitHolder u : DataHolder.getSingleton().getUnits()) {
            Integer cnt = forceTable.get(u);
            if ((cnt != null) && (cnt > 0)) {
                buffer.append(nf.format(cnt));
                buffer.append(" " + u.getName() + "\n");
            }
        }
        JOptionPaneHelper.showInformationBox(jResultDialog, buffer.toString(), "Maximale Kampfkraft");
    }

    private List<Attack> getSelectedSupports() {
        final List<Attack> selectedSupports = new LinkedList<Attack>();
        int[] selectedRows = jSupportTable.getSelectedRows();
        if (selectedRows != null && selectedRows.length < 1) {
            return selectedSupports;
        }

        for (Integer selectedRow : selectedRows) {
            Village source = (Village) jSupportTable.getValueAt(selectedRow, 0);
            UnitHolder unit = (UnitHolder) jSupportTable.getValueAt(selectedRow, 1);
            Date sendTime = (Date) jSupportTable.getValueAt(selectedRow, 2);
            Attack a = new Attack();
            a.setSource(source);
            a.setTarget(mCurrentVillage);
            a.setUnit(unit);
            a.setArriveTime(new Date(sendTime.getTime() + (long) (DSCalculator.calculateMoveTimeInSeconds(source, mCurrentVillage, unit.getSpeed()) * 1000)));
            a.setType(Attack.SUPPORT_TYPE);
            selectedSupports.add(a);
        }
        return selectedSupports;
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private de.tor.tribes.ui.CapabilityInfoPanel capabilityInfoPanel1;
    private de.tor.tribes.ui.components.DateTimeField dateTimeField;
    private org.jdesktop.swingx.JXCollapsiblePane infoPanel;
    private javax.swing.JTextField jArriveTime;
    private javax.swing.JComboBox jAttackPlansBox;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton10;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton4;
    private javax.swing.JButton jButton5;
    private javax.swing.JButton jButton6;
    private javax.swing.JButton jButton7;
    private javax.swing.JButton jButton8;
    private javax.swing.JButton jButton9;
    private javax.swing.JCheckBox jDefOnlyBox;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JSpinner jMinUnitCountSpinner;
    private javax.swing.JTextField jNewPlanName;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JDialog jResultDialog;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane3;
    private org.jdesktop.swingx.JXTable jSupportTable;
    private javax.swing.JList jTagsList;
    private javax.swing.JTextField jTargetVillage;
    private javax.swing.JDialog jTransferToAttackOverviewDialog;
    private org.jdesktop.swingx.JXLabel jXLabel1;
    // End of variables declaration//GEN-END:variables
}
