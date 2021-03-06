// License: GPL. For details, see Readme.txt file.
package de.andre_kutzleb.osm_routing;

import java.awt.BorderLayout;
import java.awt.Color;
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.filechooser.FileFilter;

import org.openstreetmap.gui.jmapviewer.Coordinate;
import org.openstreetmap.gui.jmapviewer.JMapViewer;
import org.openstreetmap.gui.jmapviewer.JMapViewerTree;
import org.openstreetmap.gui.jmapviewer.MapMarkerDot;
import org.openstreetmap.gui.jmapviewer.OsmTileLoader;
import org.openstreetmap.gui.jmapviewer.events.JMVCommandEvent;
import org.openstreetmap.gui.jmapviewer.interfaces.JMapViewerEventListener;
import org.openstreetmap.gui.jmapviewer.interfaces.MapMarker;
import org.openstreetmap.gui.jmapviewer.interfaces.TileSource;
import org.openstreetmap.gui.jmapviewer.tilesources.BingAerialTileSource;
import org.openstreetmap.gui.jmapviewer.tilesources.MapQuestOpenAerialTileSource;
import org.openstreetmap.gui.jmapviewer.tilesources.MapQuestOsmTileSource;
import org.openstreetmap.gui.jmapviewer.tilesources.OsmTileSource;

import osm.map.Dijkstra;
import osm.map.Dijkstra.TravelType;
import osm.map.Graph;
import osmlab.OSMFileConverter;
import population.PopulationData;
import population.PopulationInfo;
import population.PopulationPoint;

/**
 * Demonstrates the usage of {@link JMapViewer}
 *
 * @author Jan Peter Stotz
 *
 */
public class OsmRoutingMain extends JFrame implements JMapViewerEventListener  {

    private static final long serialVersionUID = 1L;

    private final JMapViewerTree treeMap;


    /**
     * Constructs the {@code Demo}.
     * @param options 
     * @throws IOException 
     */
    public OsmRoutingMain(RoutingOptions options) throws IOException {
        super("JMapViewer Demo");
        setSize(400, 400);
        
      
        
        String cacheFolder = "JMapViewerCache";
        boolean doCaching = true;

		
        treeMap = new JMapViewerTree("Zones", cacheFolder, doCaching,options);

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


    	final PopulationData populationData = PopulationData.parseFromFile("deuds00ag.asc");

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
     
        final JCheckBox showPopulation = new JCheckBox("Population Overlay");
        List<MapMarker> markers = new ArrayList<>();
        showPopulation.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(showPopulation.isSelected()) {
                	float range = (float) (populationData.getMaxDensity() - populationData.getMinDensity());
        			
        			populationData.forEachCell((coords, val) -> {
        				if(val > 0) {
        					MapMarkerDot dot = new MapMarkerDot(new Coordinate(coords.getX(), coords.getY()));
        					
        					float tVal = (float) ((val - (populationData.getMinDensity()))/range);
        					tVal = (float) Math.atan(tVal*8)/1.5f;
        					System.out.println(tVal);
        					Color col = new Color(tVal,(1-tVal)*0.75f,0);
        					dot.setColor(col);
        					dot.setBackColor(col);
        					map().addMapMarker(dot);	
        					markers.add(dot);
        				}
        			});
                } else {
                	markers.forEach(m -> map().removeMapMarker(m));
                	markers.clear();
                }
            }
        });
        panelTop.add(showPopulation);
        
        
        JSlider populationModifier = new JSlider(JSlider.HORIZONTAL,0,10,0);
        populationModifier.setMajorTickSpacing(1);
        populationModifier.setPaintTicks(true);
        populationModifier.setPaintLabels(true);
        JRadioButton prefPop = new JRadioButton("Prefer Population");
        JRadioButton avoidPop = new JRadioButton("Avoid Population");
        
        prefPop.setSelected(true);
        ButtonGroup popSelect = new ButtonGroup();
        
        ActionListener l = a -> map().setPreferPopulation(prefPop.isSelected());
        l.actionPerformed(null); //trigger initial set of variable
       prefPop.addActionListener(l);
       avoidPop.addActionListener(l);
        popSelect.add(avoidPop);
        popSelect.add(prefPop);
        
        panelTop.add(prefPop);
        panelTop.add(avoidPop);
        
        
        populationModifier.addChangeListener(e -> {
        	map().setPopulationModifier(populationModifier.getValue());
        	
        });
        panelTop.add(populationModifier);
        

        panelTop.add(button);

        

        add(treeMap, BorderLayout.CENTER);

        // add progressbars
        options.getProgress().values().forEach(helpPanel::add);
        
        map().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    map().getAttribution().handleAttribution(e.getPoint(), true);
                }
            }
        });
        

		MapMarkerDot[] dots = new MapMarkerDot[5];

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
                if (showToolTip.isSelected()) {
                	PopulationInfo info = populationData.closestDataForCoordinate((float) map().getPosition(p).getLat(), (float)map().getPosition(p).getLon());
                	if(info == null) {
                		map().setToolTipText(map().getPosition(p).toString());                		
                	} else {
                		map().setToolTipText(map().getPosition(p).toString() + " : " + info.getPointOfInterest().getPopulationDensity());
                		for(int i= 0; i < dots.length; i++) {
                			if (dots[i] != null) {
                				map().removeMapMarker(dots[i]);
                			}
                			PopulationPoint pp = info.getPopulationPoint(i);
                			MapMarkerDot ppd = new MapMarkerDot(Math.round(pp.getY())+", " + Math.round(pp.getX()),new Coordinate(pp.getX(), pp.getY()));
                			map().addMapMarker(ppd);
                			dots[i] = ppd;
                		}
                	}
                }
            }
        });
    }
   
    private static RoutingOptions initializeRouting(TravelType[] travelTypes) throws IOException {
    	Graph graph = null;

		File dataFolder = new File("data");
		dataFolder.mkdirs();

		FilenameFilter folderFilter = (dir, name) -> new File(dataFolder.getAbsolutePath() + File.separator + name).isDirectory() && !new File(dataFolder.getAbsolutePath() + File.separator + name).getName().startsWith("_");
		
		String[] folders = dataFolder.list(folderFilter);
		System.out.println(dataFolder);
		if (folders.length == 0) {
			final JFileChooser fc = new JFileChooser();
			fc.setFileFilter(new FileFilter() {

				@Override
				public String getDescription() {
					return "osm.pbf files";
				}

				@Override
				public boolean accept(File f) {
					return f.getAbsolutePath().endsWith(".osm.pbf") || f.isDirectory();
				}
			});
			fc.setDialogTitle("Choose osm file");

			int returnVal = fc.showOpenDialog(null);

			if (returnVal == JFileChooser.APPROVE_OPTION) {
				OSMFileConverter
						.process(fc.getSelectedFile().getAbsoluteFile());
			} else {
				System.exit(0);
			}
		}
		
		folders = dataFolder.list(folderFilter);
		
		graph = Graph.createGraph(folders[0] +".osm.pbf");

		RoutingOptions options = new RoutingOptions(graph);
		
		for(TravelType type : travelTypes) {
			JProgressBar progress = new JProgressBar();
			progress.setVisible(false);
			progress.setStringPainted(true);
			
			options.addRoutingOption(new Dijkstra(graph, type ,progress), progress);
		}
        
		return options;
	}

	private JMapViewer map() {
        return treeMap.getViewer();
    }

    /**
     * @param args Main program arguments
     * @throws IOException 
     */
    public static void main(String[] args) throws IOException {
    	
    	TravelType[] types = new TravelType[] {/*TravelType.CAR_SHORTEST, TravelType.CAR_FASTEST, TravelType.PEDESTRIAN,*/ TravelType.PEDESTRIAN_POPULATION, TravelType.PEDESTRIAN};
    //	types = new TravelType[] {TravelType.CAR_SHORTEST_FF, TravelType.CAR_FASTEST_FF, TravelType.PEDESTRIAN_FF};

    	  	JFrame frame = new JFrame("OSM viewer");
    	    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    	    final JProgressBar aJProgressBar = new JProgressBar(JProgressBar.HORIZONTAL);
    	    aJProgressBar.setString("Loading OSM data...");
    	    aJProgressBar.setStringPainted(true);
    	    aJProgressBar.setIndeterminate(true);

    	    frame.add(aJProgressBar, BorderLayout.NORTH);
    	    frame.setSize(300, 60);
    	    frame.setLocationRelativeTo(null);
    	    frame.setVisible(true);
    	
    	
    	  RoutingOptions options = initializeRouting(types); 
    	  frame.dispose();
          if(options == null) {
          	System.err.println("Failed to load, exiting.");
          	System.exit(1);
          }
          
  
        new OsmRoutingMain(options).setVisible(true);
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
