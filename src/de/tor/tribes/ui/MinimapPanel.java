/*
 * MinimapPanel.java
 *
 * Created on 11. September 2007, 17:41
 */
package de.tor.tribes.ui;

import de.tor.tribes.io.DataHolder;
import de.tor.tribes.php.ScreenUploadInterface;
import de.tor.tribes.types.Marker;
import de.tor.tribes.types.Village;
import de.tor.tribes.util.BrowserCommandSender;
import de.tor.tribes.util.GlobalOptions;
import de.tor.tribes.util.JOptionPaneHelper;
import de.tor.tribes.util.ServerSettings;
import de.tor.tribes.util.ToolChangeListener;
import de.tor.tribes.util.mark.MarkerManager;
import de.tor.tribes.util.mark.MarkerManagerListener;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.LinkedList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import org.apache.log4j.Logger;

/**
 * @author  jejkal
 */
public class MinimapPanel extends javax.swing.JPanel implements MarkerManagerListener {

    private static Logger logger = Logger.getLogger("MinimapCanvas");
    private Image mBuffer = null;
    private int iX = 0;
    private int iY = 0;
    private int iWidth = 0;
    private int iHeight = 0;
    private MinimapZoomFrame mZoomFrame = null;
    private int iCurrentCursor = ImageManager.CURSOR_DEFAULT;
    private static MinimapPanel SINGLETON = null;
    private ScreenshotPanel mScreenshotPanel = null;
    private List<MinimapListener> mMinimapListeners = null;
    private List<ToolChangeListener> mToolChangeListeners = null;
    private boolean doRedraw = false;
    private int iXDown = 0;
    private int iYDown = 0;
    private Rectangle2D rDrag = null;
    private Rectangle rVisiblePart = null;
    boolean zoomed = false;

    public static synchronized MinimapPanel getSingleton() {
        if (SINGLETON == null) {
            SINGLETON = new MinimapPanel();
        }
        return SINGLETON;
    }

    /** Creates new form MinimapPanel */
    MinimapPanel() {
        initComponents();
        setSize(300, 300);
        mMinimapListeners = new LinkedList<MinimapListener>();
        mToolChangeListeners = new LinkedList<ToolChangeListener>();
        setCursor(ImageManager.getCursor(iCurrentCursor));
        mScreenshotPanel = new ScreenshotPanel();
        jPanel1.add(mScreenshotPanel);
        int mapWidth = (int) ServerSettings.getSingleton().getMapDimension().getWidth();
        int mapHeight = (int) ServerSettings.getSingleton().getMapDimension().getHeight();
        rVisiblePart = new Rectangle(0, 0, mapWidth, mapHeight);
        zoomed = false;
        MarkerManager.getSingleton().addMarkerManagerListener(this);
        MinimapRepaintThread.getSingleton().setVisiblePart(rVisiblePart);
        MinimapRepaintThread.getSingleton().start();


        addMouseListener(new MouseListener() {

            @Override
            public void mouseClicked(MouseEvent e) {
                Point p = mousePosToMapPosition(e.getX(), e.getY());
                DSWorkbenchMainFrame.getSingleton().centerPosition(p.x, p.y);

                if (mZoomFrame != null) {
                    if (mZoomFrame.isVisible()) {
                        mZoomFrame.toFront();
                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (iCurrentCursor == ImageManager.CURSOR_SHOT || iCurrentCursor == ImageManager.CURSOR_ZOOM) {
                    iXDown = e.getX();
                    iYDown = e.getY();
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (rDrag == null) {
                    return;
                }
                if (iCurrentCursor == ImageManager.CURSOR_SHOT) {
                    try {
                        BufferedImage i = MinimapRepaintThread.getSingleton().getBuffer();
                        int mapWidth = (int) ServerSettings.getSingleton().getMapDimension().getWidth();
                        int mapHeight = (int) ServerSettings.getSingleton().getMapDimension().getHeight();
                        int x = (int) Math.rint((double) mapWidth / (double) getWidth() * (double) rDrag.getX());
                        int y = (int) Math.rint((double) mapHeight / (double) getHeight() * (double) rDrag.getY());
                        int w = (int) Math.rint((double) mapWidth / (double) getWidth() * (double) (rDrag.getWidth() - rDrag.getX()));
                        int h = (int) Math.rint((double) mapHeight / (double) getHeight() * (double) (rDrag.getHeight() - rDrag.getY()));
                        BufferedImage sub = i.getSubimage(x, y, w, h);
                        mScreenshotPanel.setBuffer(sub);
                        jPanel1.setSize(mScreenshotPanel.getSize());
                        jPanel1.setPreferredSize(mScreenshotPanel.getSize());
                        jPanel1.setMinimumSize(mScreenshotPanel.getSize());
                        jPanel1.setMaximumSize(mScreenshotPanel.getSize());
                        jScreenshotPreview.pack();
                        jScreenshotControl.pack();
                        jScreenshotPreview.setVisible(true);
                        jScreenshotControl.setVisible(true);
                    } catch (Exception ie) {
                        logger.error("Failed to initialize mapshot", ie);
                    }
                } else if (iCurrentCursor == ImageManager.CURSOR_ZOOM) {
                    if (!zoomed) {
                        int mapWidth = (int) ServerSettings.getSingleton().getMapDimension().getWidth();
                        int mapHeight = (int) ServerSettings.getSingleton().getMapDimension().getHeight();
                        int x = (int) Math.rint((double) mapWidth / (double) getWidth() * (double) rDrag.getX());
                        int y = (int) Math.rint((double) mapHeight / (double) getHeight() * (double) rDrag.getY());
                        int w = (int) Math.rint((double) mapWidth / (double) getWidth() * (double) (rDrag.getWidth() - rDrag.getX()));

                        if (w >= 10) {
                            rVisiblePart = new Rectangle(x, y, w, w);
                            MinimapRepaintThread.getSingleton().setVisiblePart(rVisiblePart);
                            redraw();
                            zoomed = true;
                        }
                    } else {
                        int mapWidth = (int) ServerSettings.getSingleton().getMapDimension().getWidth();
                        int mapHeight = (int) ServerSettings.getSingleton().getMapDimension().getHeight();
                        rVisiblePart = new Rectangle(0, 0, mapWidth, mapHeight);
                        MinimapRepaintThread.getSingleton().setVisiblePart(rVisiblePart);
                        redraw();
                        zoomed = false;
                    }
                    mZoomFrame.setVisible(false);
                }
                iXDown = 0;
                iYDown = 0;
                rDrag = null;
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                switch (iCurrentCursor) {
                    case ImageManager.CURSOR_ZOOM: {
                        if (mZoomFrame != null) {
                            mZoomFrame.setVisible(true);
                        }
                    }
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (mZoomFrame != null) {
                    if (mZoomFrame.isVisible()) {
                        mZoomFrame.setVisible(false);
                    }
                    iXDown = 0;
                    iYDown = 0;
                    rDrag = null;
                }
            }
        });

        addMouseMotionListener(new MouseMotionListener() {

            @Override
            public void mouseDragged(MouseEvent e) {
                switch (iCurrentCursor) {
                    case ImageManager.CURSOR_MOVE: {
                        Point p = mousePosToMapPosition(e.getX(), e.getY());
                        DSWorkbenchMainFrame.getSingleton().centerPosition(p.x, p.y);
                        rDrag = null;
                        break;
                    }
                    case ImageManager.CURSOR_SHOT: {
                        rDrag = new Rectangle2D.Double(iXDown, iYDown, e.getX(), e.getY());
                        break;
                    }
                    case ImageManager.CURSOR_ZOOM: {
                        rDrag = new Rectangle2D.Double(iXDown, iYDown, e.getX(), e.getY());
                        break;
                    }
                }

            }

            @Override
            public void mouseMoved(MouseEvent e) {
                switch (iCurrentCursor) {
                    case ImageManager.CURSOR_ZOOM: {
                        if (mZoomFrame != null) {
                            if (!mZoomFrame.isVisible()) {
                                mZoomFrame.setVisible(true);
                            }
                            int mapWidth = (int) ServerSettings.getSingleton().getMapDimension().getWidth();
                            int mapHeight = (int) ServerSettings.getSingleton().getMapDimension().getHeight();

                            int x = (int) Math.rint((double) mapWidth / (double) getWidth() * (double) e.getX());
                            int y = (int) Math.rint((double) mapHeight / (double) getHeight() * (double) e.getY());
                            mZoomFrame.updatePosition(x, y);
                        }
                        break;
                    }
                    default: {
                        if (mZoomFrame != null) {
                            if (mZoomFrame.isVisible()) {
                                mZoomFrame.setVisible(false);
                            }
                        }
                    }
                }
            }
        });

        addMouseWheelListener(new MouseWheelListener() {

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                iCurrentCursor += e.getWheelRotation();
                if (iCurrentCursor == ImageManager.CURSOR_DEFAULT + e.getWheelRotation()) {
                    if (e.getWheelRotation() < 0) {
                        iCurrentCursor = ImageManager.CURSOR_SHOT;
                    } else {
                        iCurrentCursor = ImageManager.CURSOR_MOVE;
                    }
                } else if (iCurrentCursor < ImageManager.CURSOR_MOVE) {
                    iCurrentCursor = ImageManager.CURSOR_DEFAULT;
                } else if (iCurrentCursor > ImageManager.CURSOR_SHOT) {
                    iCurrentCursor = ImageManager.CURSOR_DEFAULT;
                }
                if (iCurrentCursor != ImageManager.CURSOR_ZOOM) {
                    if (mZoomFrame != null) {
                        if (mZoomFrame.isVisible()) {
                            mZoomFrame.setVisible(false);
                        }
                    }
                } else {
                    if (mZoomFrame != null) {
                        mZoomFrame.setVisible(true);
                    }
                }
                setCurrentCursor(iCurrentCursor);
            }
        });
    }

    public synchronized void addMinimapListener(MinimapListener pListener) {
        mMinimapListeners.add(pListener);
    }

    public synchronized void removeMinimapListener(MinimapListener pListener) {
        mMinimapListeners.remove(pListener);
    }

    public synchronized void addToolChangeListener(ToolChangeListener pListener) {
        mToolChangeListeners.add(pListener);
    }

    public synchronized void removeToolChangeListener(ToolChangeListener pListener) {
        mToolChangeListeners.remove(pListener);
    }

    public Point mousePosToMapPosition(double pX, double pY) {
        int x = rVisiblePart.x;
        int y = rVisiblePart.y;
        //calc dots per village
        double dpvx = (double) getWidth() / (double) rVisiblePart.width;
        double dpvy = (double) getHeight() / (double) rVisiblePart.height;
        x += (int) Math.round(pX / dpvx);
        y += (int) Math.round(pY / dpvy);
        return new Point(x, y);
    }

    public void setCurrentCursor(int pCurrentCursor) {
        iCurrentCursor = pCurrentCursor;
        setCursor(ImageManager.getCursor(iCurrentCursor));
        fireToolChangedEvents(iCurrentCursor);
    }

    public void setSelection(int pX, int pY, int pWidth, int pHeight) {
        iX = pX;
        iY = pY;
        iWidth = pWidth;
        iHeight = pHeight;
    }

    @Override
    public void paint(Graphics g) {
        try {
            Graphics2D g2d = (Graphics2D) g;
            g2d.drawImage(mBuffer, 0, 0, null);
            g2d.setColor(Color.YELLOW);
            /*int mapWidth = (int) ServerSettings.getSingleton().getMapDimension().getWidth();
            int mapHeight = (int) ServerSettings.getSingleton().getMapDimension().getHeight();

            int w = (int) Math.rint(((double) getWidth() / mapWidth) * (double) iWidth);
            int h = (int) Math.rint(((double) getHeight() / mapHeight) * (double) iHeight);

            double posX = ((double) getWidth() / mapWidth * (double) iX) - w / 2;
            double posY = ((double) getHeight() / mapHeight * (double) iY) - h / 2;

            g2d.drawRect((int) Math.rint(posX), (int) Math.rint(posY), w, h);*/

            int mapWidth = rVisiblePart.width;
            int mapHeight = rVisiblePart.height;

            int w = (int) Math.rint(((double) getWidth() / mapWidth) * (double) iWidth);
            int h = (int) Math.rint(((double) getHeight() / mapHeight) * (double) iHeight);

            double posX = ((double) getWidth() / mapWidth * (double) (iX - rVisiblePart.x)) - w / 2;
            double posY = ((double) getHeight() / mapHeight * (double) (iY - rVisiblePart.y)) - h / 2;

            g2d.drawRect((int) Math.rint(posX), (int) Math.rint(posY), w, h);


            if (iCurrentCursor == ImageManager.CURSOR_SHOT) {
                if (rDrag != null) {
                    g2d.setColor(Color.ORANGE);
                    g2d.drawRect((int) rDrag.getMinX(), (int) rDrag.getMinY(), (int) (rDrag.getWidth() - rDrag.getX()), (int) (rDrag.getHeight() - rDrag.getY()));
                }
            } else if (iCurrentCursor == ImageManager.CURSOR_ZOOM) {
                if (rDrag != null) {
                    g2d.setColor(Color.CYAN);
                    g2d.drawRect((int) rDrag.getMinX(), (int) rDrag.getMinY(), (int) (rDrag.getWidth() - rDrag.getX()), (int) (rDrag.getWidth() - rDrag.getX()));
                }
            }
            g2d.dispose();
        } catch (Exception e) {
            logger.error("Failed painting Minimap", e);
        }
    }

    public void makeScreenshot() {
        jScreenshotPreview.setVisible(true);
    }

    public void resetBuffer() {
        mBuffer = null;
        redraw();
    }

    protected void updateComplete(BufferedImage pBuffer) {
        try {
            if (mZoomFrame == null) {
                mZoomFrame = new MinimapZoomFrame(pBuffer);
                mZoomFrame.setSize(300, 300);
                mZoomFrame.setLocation(0, 0);
            }
            if (mBuffer == null) {
                mBuffer = pBuffer;
                if (pBuffer == null) {
                    MinimapRepaintThread.getSingleton().update();
                    return;
                }
                mBuffer = mBuffer.getScaledInstance(getWidth(), getHeight(), BufferedImage.SCALE_SMOOTH);
            } else if ((mBuffer.getWidth(null) != getWidth()) || (mBuffer.getHeight(null) != getHeight())) {
                mZoomFrame.setMinimap(pBuffer);
                mBuffer = mBuffer.getScaledInstance(getWidth(), getHeight(), BufferedImage.SCALE_SMOOTH);
            } else if (doRedraw) {
                mZoomFrame.setMinimap(pBuffer);
                mBuffer = pBuffer;
                mBuffer = mBuffer.getScaledInstance(getWidth(), getHeight(), BufferedImage.SCALE_SMOOTH);
            }
            doRedraw = false;
            repaint();
        } catch (Exception e) {
            System.out.println("ERROR");
            logger.error("Exception while updating Minimap", e);
        //ignore
        }
    }

    public void redraw() {
        doRedraw = true;
        try {
            MinimapRepaintThread.getSingleton().update();
        } catch (Exception e) {
        }
    }

    @Override
    public void fireMarkersChangedEvent() {
        redraw();
    }

    public synchronized void fireToolChangedEvents(int pTool) {
        for (ToolChangeListener l : mToolChangeListeners) {
            l.fireToolChangedEvent(pTool);
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScreenshotControl = new javax.swing.JFrame();
        jScalingSlider = new javax.swing.JSlider();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jFileTypeChooser = new javax.swing.JComboBox();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        jTransparancySlider = new javax.swing.JSlider();
        jButton3 = new javax.swing.JButton();
        jScreenshotPreview = new javax.swing.JDialog();
        jPanel1 = new javax.swing.JPanel();

        jScreenshotControl.setTitle("Einstellungen");
        jScreenshotControl.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                fireScreenshotControlClosingEvent(evt);
            }
        });

        jScalingSlider.setMajorTickSpacing(1);
        jScalingSlider.setMaximum(10);
        jScalingSlider.setMinimum(1);
        jScalingSlider.setPaintLabels(true);
        jScalingSlider.setPaintTicks(true);
        jScalingSlider.setSnapToTicks(true);
        jScalingSlider.setValue(1);
        jScalingSlider.setOpaque(false);
        jScalingSlider.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                fireChangeScreenshotScalingEvent(evt);
            }
        });

        jLabel1.setText("Zoom");

        jLabel2.setText("Dateityp");

        jFileTypeChooser.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "png", "gif", "jpeg" }));

        jButton1.setText("Schließen");
        jButton1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                fireCloseScreenshotEvent(evt);
            }
        });

        jButton2.setText("Speichern");
        jButton2.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                fireSaveScreenshotEvent(evt);
            }
        });

        jLabel3.setText("Legendentransparenz");

        jTransparancySlider.setMajorTickSpacing(1);
        jTransparancySlider.setMaximum(10);
        jTransparancySlider.setPaintLabels(true);
        jTransparancySlider.setPaintTicks(true);
        jTransparancySlider.setSnapToTicks(true);
        jTransparancySlider.setToolTipText("Transparenz der Legende (10 = keine Legende)");
        jTransparancySlider.setValue(0);
        jTransparancySlider.setOpaque(false);

        jButton3.setText("Online stellen");
        jButton3.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                firePutScreenOnlineEvent(evt);
            }
        });

        javax.swing.GroupLayout jScreenshotControlLayout = new javax.swing.GroupLayout(jScreenshotControl.getContentPane());
        jScreenshotControl.getContentPane().setLayout(jScreenshotControlLayout);
        jScreenshotControlLayout.setHorizontalGroup(
            jScreenshotControlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jScreenshotControlLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jScreenshotControlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jScreenshotControlLayout.createSequentialGroup()
                        .addComponent(jButton1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton2))
                    .addGroup(jScreenshotControlLayout.createSequentialGroup()
                        .addGroup(jScreenshotControlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1)
                            .addComponent(jLabel3)
                            .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jScreenshotControlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jTransparancySlider, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 236, Short.MAX_VALUE)
                            .addComponent(jScalingSlider, javax.swing.GroupLayout.DEFAULT_SIZE, 236, Short.MAX_VALUE)
                            .addComponent(jFileTypeChooser, 0, 236, Short.MAX_VALUE))))
                .addContainerGap())
        );
        jScreenshotControlLayout.setVerticalGroup(
            jScreenshotControlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jScreenshotControlLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jScreenshotControlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 54, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jScalingSlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jScreenshotControlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jTransparancySlider, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jScreenshotControlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jFileTypeChooser, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 36, Short.MAX_VALUE)
                .addGroup(jScreenshotControlLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton2)
                    .addComponent(jButton3)
                    .addComponent(jButton1))
                .addContainerGap())
        );

        jScreenshotPreview.setTitle("Vorschau");
        jScreenshotPreview.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                fireMapPreviewClosingEvent(evt);
            }
        });

        jPanel1.setBackground(new java.awt.Color(102, 255, 102));
        jPanel1.setOpaque(false);
        jPanel1.setPreferredSize(new java.awt.Dimension(0, 0));
        jPanel1.setLayout(new java.awt.BorderLayout());

        javax.swing.GroupLayout jScreenshotPreviewLayout = new javax.swing.GroupLayout(jScreenshotPreview.getContentPane());
        jScreenshotPreview.getContentPane().setLayout(jScreenshotPreviewLayout);
        jScreenshotPreviewLayout.setHorizontalGroup(
            jScreenshotPreviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jScreenshotPreviewLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 321, Short.MAX_VALUE)
                .addContainerGap())
        );
        jScreenshotPreviewLayout.setVerticalGroup(
            jScreenshotPreviewLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jScreenshotPreviewLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 248, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 304, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 142, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

private void fireChangeScreenshotScalingEvent(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_fireChangeScreenshotScalingEvent
    mScreenshotPanel.setScaling(jScalingSlider.getValue());
    jPanel1.setSize(mScreenshotPanel.getSize());
    jPanel1.setPreferredSize(mScreenshotPanel.getSize());
    jPanel1.setMinimumSize(mScreenshotPanel.getSize());
    jPanel1.setMaximumSize(mScreenshotPanel.getSize());
    jScreenshotPreview.pack();
}//GEN-LAST:event_fireChangeScreenshotScalingEvent

private void fireCloseScreenshotEvent(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fireCloseScreenshotEvent
    jScreenshotPreview.setVisible(false);
    jScreenshotControl.setVisible(false);
}//GEN-LAST:event_fireCloseScreenshotEvent

private void fireSaveScreenshotEvent(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_fireSaveScreenshotEvent
    String dir = GlobalOptions.getProperty("screen.dir");
    if (dir == null) {
        dir = ".";
    }
    JFileChooser chooser = null;
    try {
        chooser = new JFileChooser(dir);
    } catch (Exception e) {
        JOptionPaneHelper.showErrorBox(this, "Konnte Dateiauswahldialog nicht öffnen.\nMöglicherweise verwendest du Windows Vista. Ist dies der Fall, beende DS Workbench, klicke mit der rechten Maustaste auf DSWorkbench.exe,\n" +
                "wähle 'Eigenschaften' und deaktiviere dort unter 'Kompatibilität' den Windows XP Kompatibilitätsmodus.", "Fehler");
        return;
    }
    chooser.setDialogTitle("Speichern unter...");
    chooser.setSelectedFile(new File("map"));

    final String type = (String) jFileTypeChooser.getSelectedItem();
    chooser.setFileFilter(new FileFilter() {

        @Override
        public boolean accept(File f) {
            if ((f != null) && (f.isDirectory() || f.getName().endsWith(type))) {
                return true;
            }
            return false;
        }

        @Override
        public String getDescription() {
            return "*." + type;
        }
    });
    int ret = chooser.showSaveDialog(jScreenshotControl);
    if (ret == JFileChooser.APPROVE_OPTION) {
        try {
            File f = chooser.getSelectedFile();
            String file = f.getCanonicalPath();
            if (!file.endsWith(type)) {
                file += "." + type;
            }
            File target = new File(file);
            if (target.exists()) {
                //ask if overwrite
                if (JOptionPaneHelper.showQuestionConfirmBox(jScreenshotControl, "Existierende Datei überschreiben?", "Überschreiben", "Nein", "Ja") != JOptionPane.YES_OPTION) {
                    return;
                }
            }
            ImageIO.write(mScreenshotPanel.getResult(jTransparancySlider.getValue()), type, target);
            GlobalOptions.addProperty("screen.dir", target.getParent());
        } catch (Exception e) {
            logger.error("Failed to write map shot", e);
        }
    }
}//GEN-LAST:event_fireSaveScreenshotEvent

private void fireMapPreviewClosingEvent(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_fireMapPreviewClosingEvent
    jScreenshotControl.setVisible(false);
}//GEN-LAST:event_fireMapPreviewClosingEvent

private void fireScreenshotControlClosingEvent(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_fireScreenshotControlClosingEvent
    jScreenshotPreview.setVisible(false);
}//GEN-LAST:event_fireScreenshotControlClosingEvent

private void firePutScreenOnlineEvent(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_firePutScreenOnlineEvent
    try {
        ImageIO.write(mScreenshotPanel.getResult(jTransparancySlider.getValue()), "png", new File("tmp.png"));
    } catch (Exception e) {
        JOptionPaneHelper.showErrorBox(jScreenshotControl, "Fehler beim Speichern der Grafik", "Fehler");
        return;
    }
    String result = ScreenUploadInterface.upload("tmp.png");
    if (result != null) {
        if (result.indexOf("view.php") > 0) {
            try {
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(result), null);
                JOptionPaneHelper.showInformationBox(jScreenshotControl, "Kartengrafik erfolgreich Online gestellt.\n" +
                        "Der Zugriffslink (" + result + ")\n" +
                        "wurde in die Zwischenablage kopiert.", "Information");
                BrowserCommandSender.openPage(result);
            } catch (Exception e) {
                JOptionPaneHelper.showWarningBox(jScreenshotControl, "Fehler beim Kopieren des Links in die Zwischenablage." +
                        "Der Zugriffslink lautet: " + result, "Warnung");
            }
        } else {
            JOptionPaneHelper.showErrorBox(this, "Kartengrafik konnte nicht Online gestellt werden.\n" +
                    "Fehler: " + result, "Fehler");
        }
    }

}//GEN-LAST:event_firePutScreenOnlineEvent

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JComboBox jFileTypeChooser;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JSlider jScalingSlider;
    private javax.swing.JFrame jScreenshotControl;
    private javax.swing.JDialog jScreenshotPreview;
    private javax.swing.JSlider jTransparancySlider;
    // End of variables declaration//GEN-END:variables
}

class MinimapRepaintThread extends Thread {

    private static Logger logger = Logger.getLogger("MinimapRenderer");
    private BufferedImage mBuffer = null;
    private boolean drawn = false;
    private Dimension mapDim = null;
    private static MinimapRepaintThread SINGLETON = null;
    private Rectangle visiblePart = null;

    public static synchronized MinimapRepaintThread getSingleton() {
        if (SINGLETON == null) {
            try {
                SINGLETON = new MinimapRepaintThread();
            } catch (Exception e) {
                SINGLETON = null;
            }
        }

        return SINGLETON;
    }

    MinimapRepaintThread() {
    }

    public void setVisiblePart(Rectangle pVisible) {
        visiblePart = (Rectangle) pVisible.clone();
    }

    public void update() {
        Dimension currentDim = ServerSettings.getSingleton().getMapDimension();
        if (currentDim == null) {
            return;
        }
        if ((mapDim == null) || (mapDim.width != currentDim.width) || (mapDim.height != currentDim.height)) {
            if (mapDim == null) {
                mapDim = (Dimension) currentDim.clone();
            } else {
                mapDim.setSize(currentDim);
            }
            mBuffer = new BufferedImage(mapDim.width, mapDim.height, BufferedImage.TYPE_INT_RGB);
        }
        drawn = false;
    }

    protected BufferedImage getBuffer() {
        return mBuffer;
    }

    @Override
    public void run() {
        while (true) {
            try {
                if (!drawn) {
                    drawn = redraw();
                }

                MinimapPanel.getSingleton().updateComplete(mBuffer);
                try {
                    Thread.sleep(100);
                } catch (Exception e) {
                }
            } catch (Exception oe) {
                if (mBuffer == null) {
                    update();
                } else {
                    logger.error("Failed to re-render minimap", oe);
                }
            }
        }
    }

    private boolean redraw() {
        Village[][] mVisibleVillages = DataHolder.getSingleton().getVillages();

        if (mVisibleVillages == null) {
            return false;
        }

        Graphics2D g2d = (Graphics2D) mBuffer.getGraphics();
        g2d.setColor(new Color(35, 125, 0));
        g2d.fillRect(0, 0, mBuffer.getWidth(null), mBuffer.getHeight(null));

        boolean markPlayer = false;
        try {
            markPlayer = Boolean.parseBoolean(GlobalOptions.getProperty("mark.villages.on.minimap"));
        } catch (Exception e) {
            markPlayer = false;
        }

        boolean markedOnly = false;
        try {
            markedOnly = Boolean.parseBoolean(GlobalOptions.getProperty("draw.marked.only"));
        } catch (Exception e) {
            markedOnly = false;
        }

        if (ServerSettings.getSingleton().getMapDimension() == null) {
            //could not draw minimap if dimensions are not loaded yet
            return false;
        }
        boolean showBarbarian = true;
        try {
            showBarbarian = Boolean.parseBoolean(GlobalOptions.getProperty("show.barbarian"));
        } catch (Exception e) {
        }
        Color DEFAULT = Color.WHITE;
        try {
            if (Integer.parseInt(GlobalOptions.getProperty("default.mark")) == 1) {
                DEFAULT = Color.RED;
            }
        } catch (Exception e) {
            DEFAULT = Color.WHITE;
        }

        double wField = ServerSettings.getSingleton().getMapDimension().getWidth() / (double) visiblePart.width;
        double hField = ServerSettings.getSingleton().getMapDimension().getHeight() / (double) visiblePart.height;

        //  for (int i = 0; i < ServerSettings.getSingleton().getMapDimension().getWidth(); i++) {
        //  for (int j = 0; j < ServerSettings.getSingleton().getMapDimension().getHeight(); j++) {
        for (int i = visiblePart.x; i < (visiblePart.width + visiblePart.x); i++) {
            for (int j = visiblePart.y; j < (visiblePart.height + visiblePart.y); j++) {
                Village v = mVisibleVillages[i][j];
                if (v != null) {
                    Color mark = null;
                    boolean isLeft = false;
                    if (v.getTribe() == null) {
                        isLeft = true;
                    } else {
                        Village currentUserVillage = DSWorkbenchMainFrame.getSingleton().getCurrentUserVillage();
                        if ((currentUserVillage != null) && (v.getTribe().toString().equals(currentUserVillage.getTribe().toString()))) {
                            //village is owned by current player. mark it dependent on settings
                            if ((markPlayer) || (markedOnly)) {
                                mark = Color.YELLOW;
                            }
                        } else {
                            try {
                                Marker m = MarkerManager.getSingleton().getMarker(v.getTribe());
                                if (m == null) {
                                    m = MarkerManager.getSingleton().getMarker(v.getTribe().getAlly());
                                    if (m != null) {
                                        mark = m.getMarkerColor();
                                    }
                                } else {
                                    mark = m.getMarkerColor();
                                }
                            } catch (Exception e) {
                                mark = null;
                            }
                        }
                    }

                    if (!isLeft) {
                        if (mark != null) {
                            g2d.setColor(mark);
                        } else {
                            if (!markedOnly) {
                                g2d.setColor(DEFAULT);
                            } else {
                                g2d.setColor(new Color(35, 125, 0));
                            }
                        }
                        // g2d.fillRect(i, j, 1, 1);
                        g2d.fillRect((int) Math.round((i - visiblePart.x) * wField), (int) Math.round((j - visiblePart.y) * hField), (int) Math.round(wField), (int) Math.round(hField));
                    } else {
                        if (showBarbarian) {
                            g2d.setColor(Color.BLACK);
                            //  g2d.fillRect(i, j, 1, 1);
                            g2d.fillRect((int) Math.round((i - visiblePart.x) * wField), (int) Math.round((j - visiblePart.y) * hField), (int) Math.round(wField), (int) Math.round(hField));
                        }
                    }
                }
            }
        }

        try {
            if (Boolean.parseBoolean(GlobalOptions.getProperty("map.showcontinents"))) {
                g2d.setColor(Color.BLACK);
                Composite c = g2d.getComposite();
                Composite a = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.4f);
                Font f = g2d.getFont();
                Font t = new Font("Serif", Font.BOLD, (int) Math.round(30 * hField));
                int coordType = ServerSettings.getSingleton().getCoordType();
                if (coordType != 2) {
                    t = new Font("Serif", Font.BOLD, 20);
                }
                g2d.setFont(t);
                int fact = 10;
                int mid = 50;
                if (coordType != 2) {
                    fact = 5;
                    mid = 25;
                }

                mid = (int) Math.round(mid * wField);

                for (int i = 0; i < 10; i++) {
                    for (int j = 0; j < 10; j++) {
                        g2d.setComposite(a);

                        String conti = "K" + (j * 10 + i);
                        Rectangle2D bounds = g2d.getFontMetrics(t).getStringBounds(conti, g2d);
                        int cx = i * fact * 10 - visiblePart.x;
                        int cy = j * fact * 10 - visiblePart.y;
                        cx = (int) Math.round(cx * wField);
                        cy = (int) Math.round(cy * hField);
                        g2d.drawString(conti, (int) Math.rint(cx + mid - bounds.getWidth() / 2), (int) Math.rint(cy + mid + bounds.getHeight() / 2));
                        g2d.setComposite(c);
                        int wk = 100;
                        int hk = 100;

                        if (coordType != 2) {
                            wk = 50;
                            hk = 50;
                        }
                        if (i == 9) {
                            wk -= 1;
                        }
                        if (j == 9) {
                            hk -= 1;
                        }



                        g2d.drawRect(cx, cy, (int) Math.round(wk * wField), (int) Math.round(hk * hField));
                    }
                }
                /*  for (int i = 0; i < 10; i++) {
                for (int j = 0; j < 10; j++) {
                g2d.setComposite(a);

                String conti = "K" + (j * 10 + i);
                Rectangle2D bounds = g2d.getFontMetrics(t).getStringBounds(conti, g2d);

                g2d.drawString(conti, (int) Math.rint(i * fact * 10 + mid - bounds.getWidth() / 2), (int) Math.rint(j * fact * 10 + mid + bounds.getHeight() / 2));
                g2d.setComposite(c);
                int wk = 100;
                int hk = 100;

                if (coordType != 2) {
                wk = 50;
                hk = 50;
                }
                if (i == 9) {
                wk -= 1;
                }
                if (j == 9) {
                hk -= 1;
                }

                int cx = (i * fact * 10);
                int cy = (j * fact * 10);
                g2d.drawRect(cx, cy, wk, hk);
                }
                }*/
                g2d.setFont(f);
            }
        } catch (Exception e) {
            logger.error("Creation of Minimap failed", e);
        }
        g2d.dispose();
        return true;
    }
}

