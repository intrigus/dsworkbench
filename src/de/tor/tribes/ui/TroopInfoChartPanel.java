/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * TroopInfoChartPanel.java
 *
 * Created on 03.06.2009, 11:39:10
 */
package de.tor.tribes.ui;

import de.tor.tribes.types.Village;
import de.tor.tribes.util.Constants;
import de.tor.tribes.util.troops.TroopsManager;
import de.tor.tribes.util.troops.VillageTroopsHolder;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.LinkedList;
import java.util.List;
import javax.swing.JFrame;
import de.tor.tribes.ui.models.TroopsManagerTableModel;
import java.awt.geom.Arc2D;

/**
 *
 * @author Charon
 */
public class TroopInfoChartPanel extends javax.swing.JPanel {

    private Village village = null;

    /** Creates new form TroopInfoChartPanel */
    public TroopInfoChartPanel() {
        initComponents();
    }

    public void setVillage(Village v) {
        village = v;
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        Graphics2D g2d = (Graphics2D) g;
        List<Color> c = new LinkedList<Color>();
        c.add(Color.RED);
        c.add(Color.GREEN);
        c.add(Color.YELLOW);
        c.add(Color.BLUE);
        VillageTroopsHolder holder = null;
        g2d.setColor(Constants.DS_ROW_B);
        g2d.fillRect(0, 0, getBounds().width, getBounds().height);
        if (village != null) {
            holder = TroopsManager.getSingleton().getTroopsForVillage(village);
        }
        if (holder != null) {
            int own = holder.getTroopPopCount(TroopsManagerTableModel.SHOW_OWN_TROOPS);
            int inVillage = holder.getTroopPopCount(TroopsManagerTableModel.SHOW_TROOPS_IN_VILLAGE);
            int outside = holder.getTroopPopCount(TroopsManagerTableModel.SHOW_TROOPS_OUTSIDE);
            int ontheway = holder.getTroopPopCount(TroopsManagerTableModel.SHOW_TROOPS_ON_THE_WAY);
            //invillage(own, foreign), outside, ontheway
            int overall = inVillage + outside + ontheway;
            double percInVillage = (inVillage != 0) ? ((double) inVillage / (double) overall) : 0;
            double percOwn = (own != 0) ? ((double) own / (double) overall) : 0;
            double percOut = (outside != 0) ? ((double) outside / (double) overall) : 0;
            double percOnTheWay = (ontheway != 0) ? ((double) ontheway / (double) overall) : 0;

            g2d.setColor(Color.GREEN);
            g2d.fillArc(0, 0, getWidth(), getWidth(), 0, (int) Math.rint(360 * percInVillage));
            g2d.setColor(Color.BLUE);
            g2d.fillArc(0, 0, getWidth(), getWidth(), 0, (int) Math.rint(360 * percOwn));
            g2d.setColor(Color.YELLOW);
            g2d.fillArc(0, 0, getWidth(), getWidth(), (int) Math.rint(360 * percInVillage), (int) Math.rint(360 * percOut));
            g2d.setColor(Color.RED);
            g2d.fillArc(0, 0, getWidth(), getWidth(), (int) Math.rint(360 * percInVillage) + (int) Math.rint(360 * percOut), (int) Math.rint(360 * percOnTheWay));
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 400, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 300, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    public static void main(String[] args) {
        JFrame f = new JFrame();
        f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        f.add(new TroopInfoChartPanel());
        // f.setSize(300, 300);
        f.pack();
        f.setVisible(true);
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    // End of variables declaration//GEN-END:variables
}
