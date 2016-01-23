// License: GPL. For details, see Readme.txt file.
package de.jgrunert.osm_routing;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FilenameFilter;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import org.openstreetmap.gui.jmapviewer.Coordinate;
import org.openstreetmap.gui.jmapviewer.JMapViewer;
import org.openstreetmap.gui.jmapviewer.JMapViewerTree;
import org.openstreetmap.gui.jmapviewer.OsmTileLoader;
import org.openstreetmap.gui.jmapviewer.events.JMVCommandEvent;
import org.openstreetmap.gui.jmapviewer.interfaces.JMapViewerEventListener;
import org.openstreetmap.gui.jmapviewer.interfaces.TileLoader;
import org.openstreetmap.gui.jmapviewer.interfaces.TileSource;
import org.openstreetmap.gui.jmapviewer.tilesources.BingAerialTileSource;
import org.openstreetmap.gui.jmapviewer.tilesources.MapQuestOpenAerialTileSource;
import org.openstreetmap.gui.jmapviewer.tilesources.MapQuestOsmTileSource;
import org.openstreetmap.gui.jmapviewer.tilesources.OsmTileSource;

/**
 * Demonstrates the usage of {@link JMapViewer}
 *
 * @author Jan Peter Stotz
 *
 */
public class OsmRoutingMain extends JFrame implements JMapViewerEventListener  {

    private static final long serialVersionUID = 1L;

    private final JMapViewerTree treeMap;

    private final JProgressBar pedestrianProgress;
    private final JProgressBar carShortestProgress;
    private final JProgressBar carFastestProgress;
    

    /**
     * Constructs the {@code Demo}.
     */
    public OsmRoutingMain() {
        super("JMapViewer Demo");
        setSize(400, 400);
        
        String cacheFolder = "JMapViewerCache";
        boolean doCaching = true;

        pedestrianProgress = new JProgressBar();
        carShortestProgress = new JProgressBar();
        carFastestProgress = new JProgressBar();
        pedestrianProgress.setVisible(false);
        carShortestProgress.setVisible(false);
        carFastestProgress.setVisible(false);
        
		pedestrianProgress.setStringPainted(true);
		carShortestProgress.setStringPainted(true);
		carFastestProgress.setStringPainted(true);
		
        treeMap = new JMapViewerTree("Zones", cacheFolder, doCaching,pedestrianProgress,carShortestProgress,carFastestProgress);

        // Listen to the map viewer for user operations so components will
        // receive events and update
        map().addJMVListener(this);

        setLayout(new BorderLayout());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        JPanel panel = new JPanel();
        JPanel panelTop = new JPanel();

        JPanel helpPanel = new JPanel();

 
 
        add(panel, BorderLayout.NORTH);
        add(helpPanel, BorderLayout.SOUTH);
        panel.setLayout(new BorderLayout());
        panel.add(panelTop, BorderLayout.NORTH);
        JLabel helpLabel = new JLabel("Hold LMB: Move  |  Mouse Wheel: zoom  |  Left click: Start  |  Right click: Destination");
        helpPanel.add(helpLabel);
        JButton button = new JButton("Center Map on Route");
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                map().setDisplayToFitMapMarkers();
            }
        });
        JComboBox<TileSource> tileSourceSelector = new JComboBox<>(new TileSource[] {
        		new OsmTileSource.Mapnik(),
        		new MapQuestOsmTileSource(),
        		new MapQuestOpenAerialTileSource(),
                new OsmTileSource.CycleMap(),
                new BingAerialTileSource()
                 });
        tileSourceSelector.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                map().setTileSource((TileSource) e.getItem());
            }
        });
        panelTop.add(tileSourceSelector);

//        String[] files = new File(System.getProperty("user.dir")).list((dir,name) -> name.endsWith("osm.pbf"));
//        JComboBox<String> osmFileSelector = new JComboBox<String>(files);
//        panelTop.add(osmFileSelector);
        
        map().setTileLoader(new OsmTileLoader(map(), cacheFolder, doCaching));
        map().setMapMarkerVisible(true);
        map().setZoomContolsVisible(true);

        final JCheckBox showToolTip = new JCheckBox("ToolTip visible");
        showToolTip.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                map().setToolTipText(null);
            }
        });
        panelTop.add(showToolTip);

        

        panelTop.add(button);

        

        add(treeMap, BorderLayout.CENTER);

    
        helpPanel.add(pedestrianProgress);
        helpPanel.add(carShortestProgress);
        helpPanel.add(carFastestProgress);
    
        
        map().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    map().getAttribution().handleAttribution(e.getPoint(), true);
                }
            }
        });

        map().addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                Point p = e.getPoint();
                boolean cursorHand = map().getAttribution().handleAttributionCursor(p);
                if (cursorHand) {
                    map().setCursor(new Cursor(Cursor.HAND_CURSOR));
                } else {
                    map().setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                }
                if (showToolTip.isSelected()) map().setToolTipText(map().getPosition(p).toString());
            }
        });
    }

    private JMapViewer map() {
        return treeMap.getViewer();
    }

    /**
     * @param args Main program arguments
     */
    public static void main(String[] args) {
        new OsmRoutingMain().setVisible(true);
    }

    private void updateZoomParameters() {
       }

    @Override
    public void processCommand(JMVCommandEvent command) {
        if (command.getCommand().equals(JMVCommandEvent.COMMAND.ZOOM) ||
                command.getCommand().equals(JMVCommandEvent.COMMAND.MOVE)) {
            updateZoomParameters();
        }
    }
}
