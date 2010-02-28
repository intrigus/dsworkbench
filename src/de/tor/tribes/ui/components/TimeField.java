/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.tor.tribes.ui.components;

import java.awt.Color;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicArrowButton;
import org.netbeans.lib.awtextra.AbsoluteConstraints;
import org.netbeans.lib.awtextra.AbsoluteLayout;

/**
 *
 * @author Torridity
 */
public class TimeField extends JPanel {

    private final JTextField timeText;
    private final JButton timeDropdownButton;
    private TimePicker dp;
    private JDialog dlg;
    private static SimpleDateFormat format = new SimpleDateFormat("HH:mm 'Uhr'");

    final class Listener extends ComponentAdapter {

        public void componentHidden(ComponentEvent componentevent) {
            Date date = ((TimePicker) componentevent.getSource()).getTime();
            if (null != date) {
                timeText.setText(TimeField.dateToTimeString(date));
            }
            dlg.dispose();
        }

        Listener() {
        }
    }

    public TimeField() {
        timeText = new JTextField();
        timeDropdownButton = new BasicArrowButton(BasicArrowButton.SOUTH);
        init();
    }

    public TimeField(Date date) {
        timeText = new JTextField();
        timeDropdownButton = new BasicArrowButton(BasicArrowButton.SOUTH);
        init();
        timeText.setText(dateToTimeString(date));
    }

    public Date getDate() {
        return stringToTime(timeText.getText());
    }

    private void init() {
        setLayout(new AbsoluteLayout());
        timeText.setText("");
        timeText.setEditable(false);
        timeText.setBackground(new Color(255, 255, 255));
        add(timeText, new AbsoluteConstraints(0, 0, 141, 20));
        timeDropdownButton.setText("...");
        timeDropdownButton.setMargin(new Insets(2, 2, 2, 2));
        timeDropdownButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent actionevent) {
                onButtonClick(actionevent);
            }
        });
        add(timeDropdownButton, new AbsoluteConstraints(151, 0, 20, 21));

        timeText.setText("");
        timeText.setEditable(false);
        timeText.setBackground(new Color(255, 255, 255));
    }

    private void onButtonClick(ActionEvent actionevent) {
        if (actionevent.getSource() == timeDropdownButton) {
            if ("".equals(timeText.getText())) {
                dp = new TimePicker(Calendar.getInstance().getTime());
            } else {
                dp = new TimePicker(stringToTime(timeText.getText()));
            }
            dp.addComponentListener(new Listener());
            Point point = timeText.getLocationOnScreen();
            point.setLocation(point.getX(), (point.getY() - 1.0D) + timeText.getSize().getHeight());
            dlg = new JDialog(new JFrame(), true);
            dp.setParent(dlg);
            dlg.setLocation(point);
            // dlg.setResizable(false);
            dlg.setUndecorated(true);
            //JPanel p = new JPanel();
            // p.add(dp);

            dlg.getContentPane().add(dp);
            dlg.pack();
            dlg.setVisible(true);
        }
    }

    private static String dateToTimeString(Date date) {
        if (null != date) {
            return format.format(date);
        } else {
            return null;
        }
    }

    private static Date stringToTime(String s) {
        try {
            return format.parse(s);
        } catch (ParseException e) {
            return null;
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
        }
        JFrame f = new JFrame();
        f.add(new TimeField(Calendar.getInstance().getTime()));
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.pack();
        f.setVisible(true);
    }
}