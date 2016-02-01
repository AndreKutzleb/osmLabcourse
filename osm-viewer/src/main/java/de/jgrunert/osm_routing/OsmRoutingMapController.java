// License: GPL. For details, see Readme.txt file.
package de.jgrunert.osm_routing;

import it.unimi.dsi.fastutil.ints.IntList;

import java.awt.Color;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

import javax.swing.JFileChooser;
import javax.swing.JProgressBar;
import javax.swing.filechooser.FileFilter;

import org.openstreetmap.gui.jmapviewer.Coordinate;
import org.openstreetmap.gui.jmapviewer.JMapController;
import org.openstreetmap.gui.jmapviewer.JMapViewer;
import org.openstreetmap.gui.jmapviewer.MapMarkerDot;
import org.openstreetmap.gui.jmapviewer.MapPolygonImpl;
import org.openstreetmap.gui.jmapviewer.interfaces.ICoordinate;

import osm.map.Dijkstra;
import osm.map.Dijkstra.TravelType;
import osm.map.Graph;
import osm.map.GraphClickFinder;
import osm.map.Route;
import osmlab.OSMFileConverter;
import osmlab.sink.GeoUtils.FloatPoint;
import de.andre_kutzleb.osmlab.data.DijkstraWorker;

/**
 * Default map controller which implements map moving by pressing the right
 * mouse button and zooming by double click or by mouse wheel.
 *
 * @author Jan Peter Stotz
 *
 */
public class OsmRoutingMapController extends JMapController implements
		MouseListener, MouseMotionListener, MouseWheelListener {

	private static final int MOUSE_BUTTONS_MASK = MouseEvent.BUTTON3_DOWN_MASK
			| MouseEvent.BUTTON1_DOWN_MASK | MouseEvent.BUTTON2_DOWN_MASK;

	private static final int MAC_MOUSE_BUTTON3_MASK = MouseEvent.CTRL_DOWN_MASK
			| MouseEvent.BUTTON1_DOWN_MASK;

	private Point lastDragPoint;

	private boolean isMoving = false;

	private boolean movementEnabled = true;

	private int movementMouseButton = MouseEvent.BUTTON1;
	private int movementMouseButtonMask = MouseEvent.BUTTON1_DOWN_MASK;

	private boolean wheelZoomEnabled = true;
	private boolean doubleClickZoomEnabled = true;

	
	private final Graph graph;
	private final Map<TravelType, Dijkstra> dijkstras = new HashMap<>();

//	private final Progress progress;
	
	public static class Progress {
		public final JProgressBar ped;
		public final JProgressBar carS; 
		public final JProgressBar carF;
		public final JProgressBar hop;
	
		public Progress(JProgressBar ped, JProgressBar carS,
				JProgressBar carF, JProgressBar hop) {
			this.ped = ped;
			this.carS = carS;
			this.carF = carF;
			this.hop = hop;
		}	
	}

	public OsmRoutingMapController(JMapViewer map, Progress progress) throws IOException {
		super(map);
//		this.progress = progress;

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

		this.graph = graph;
		dijkstras.put(TravelType.PEDESTRIAN, new Dijkstra(graph, TravelType.PEDESTRIAN,progress.ped));
		dijkstras.put(TravelType.CAR_SHORTEST, new Dijkstra(graph, TravelType.CAR_SHORTEST,progress.carS));
		dijkstras.put(TravelType.CAR_FASTEST, new Dijkstra(graph, TravelType.CAR_FASTEST,progress.carF));
		dijkstras.put(TravelType.HOP_DISTANCE, new Dijkstra(graph, TravelType.HOP_DISTANCE,progress.hop));
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		if (!movementEnabled || !isMoving)
			return;
		// Is only the selected mouse button pressed?
		if ((e.getModifiersEx() & MOUSE_BUTTONS_MASK) == movementMouseButtonMask
				|| isPlatformOsx()
				&& e.getModifiersEx() == MAC_MOUSE_BUTTON3_MASK) {
			Point p = e.getPoint();
			if (lastDragPoint != null) {
				int diffx = lastDragPoint.x - p.x;
				int diffy = lastDragPoint.y - p.y;
				map.moveMap(diffx, diffy);
			}
			lastDragPoint = p;
		}
	}

	MapMarkerDot startDot = null;
	MapMarkerDot stopDot = null;
	int stopDotNode = 0;

	@Override
	public void mouseClicked(MouseEvent e) {

		if (e.getClickCount() == 1) {

			// Waypoint selection
			ICoordinate clickPt = map.getPosition(e.getPoint());

			int clickNextPt = new GraphClickFinder(graph).findClosestNodeTo(
					(float) clickPt.getLat(), (float) clickPt.getLon(),true);


			
			boolean leftMouse = e.getButton() == MouseEvent.BUTTON1;

			if (leftMouse) {
				try {
					onLeftClick(clickNextPt);
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				map.removeMapMarker(startDot);
				startDot = new MapMarkerDot("Start", new Coordinate(
						graph.latOf(clickNextPt), graph.lonOf(clickNextPt)));
				map.addMapMarker(startDot);

			} else {
				onRightClick(clickNextPt);
				map.removeMapMarker(stopDot);
				stopDot = new MapMarkerDot("Destination", new Coordinate(
						graph.latOf(clickNextPt), graph.lonOf(clickNextPt)));
				stopDotNode = clickNextPt;
				map.addMapMarker(stopDot);

			}
			if (true) {
				return;
			}	
		}
	}
	
	private final Semaphore dijkstraMutex = new Semaphore(4);
	boolean canRoute = false;

	private final Map<TravelType, DijkstraWorker> dijkstraWorkerRefs = Collections.synchronizedMap(new HashMap<>());
	

	private void onLeftClick(int startNode) throws InterruptedException {

		boolean canCalculate = dijkstraMutex.tryAcquire(dijkstras.size());

		if (!canCalculate) {
			// currently running calculation. stop it.
			dijkstraWorkerRefs.values().forEach(worker -> worker.cancel(true));
			// will wait for the old tasks to stop
			dijkstraMutex.acquire(dijkstras.size());
		}
		canRoute = false;

		Map<TravelType,DijkstraWorker> localWorkers = new HashMap<>();
		dijkstras.values().forEach(dijkstra -> localWorkers.put(dijkstra.travelType, new DijkstraWorker(dijkstra, startNode, dijkstraMutex)));
		dijkstraWorkerRefs.putAll(localWorkers);
		dijkstras.values().forEach(dijkstra -> dijkstra.progress.setVisible(true));
		
		PropertyChangeListener p = (c) -> {
			dijkstras.values().forEach(dijkstra -> {
				DijkstraWorker responsibleWorker = localWorkers.get(dijkstra.travelType);
				dijkstra.progress.setValue(responsibleWorker.getProgress());	
				dijkstra.progress.setString(dijkstra.getName() +": " + responsibleWorker.getProgress() + "%");
			});

			double avgPercent = localWorkers.values().stream().mapToInt(DijkstraWorker::getProgress).average().getAsDouble();
	
			// draw temporary direct air line
			if (stopDot != null) {
				drawTempLine(startNode, stopDotNode, (float) avgPercent);
			}
			
			boolean stop = !localWorkers.values().stream().filter(d -> !d.isDone()).findAny().isPresent();

			boolean cancel = localWorkers.values().stream().filter(DijkstraWorker::isCancelled).findAny().isPresent();

			if (stop) {
				
				canRoute = true;
			}

			if (stopDot != null && !cancel) {
				// in case there is already a destination,
				// instantly show the path
				onRightClick(stopDotNode);
			}

		};

		localWorkers.values().forEach(worker -> worker.addPropertyChangeListener(p));
		localWorkers.values().forEach(DijkstraWorker::execute);
	
		map.removeAllMapPolygons();
	}

	private void drawTempLine(int fromNode, int toNode, float percent) {
		map.removeAllMapPolygons();
		Coordinate a = new Coordinate(graph.latOf(fromNode),
				graph.lonOf(fromNode));
		FloatPoint percentPoint = graph.pointAtPercent(fromNode, toNode,
				percent);
		Coordinate b = new Coordinate(percentPoint.lat, percentPoint.lon);

		MapPolygonImpl routPoly = new MapPolygonImpl("", a, b, b);
		// Stroke dashed = new BasicStroke(2.0f, BasicStroke.CAP_BUTT,
		// BasicStroke.JOIN_MITER, 10.0f, new float[]{5f}, 0.0f);
		//
		int perc = Math.round(percent * 100);
		routPoly.setName(perc + "%");
		routPoly.setColor(Color.DARK_GRAY);
		map.addMapPolygon(routPoly);
	}

	private boolean onRightClick(int destinationNode) {

		boolean gotLock = dijkstraMutex.tryAcquire(4);

		if (!gotLock) {
			return false;
		}

		if (!canRoute) {
			dijkstraMutex.release(4);
			return false;
		}
		dijkstras.values().forEach(d -> {
			System.out.println(d.travelType.getName() + " reset    time: " + d.resetDuration + "ms");
			System.out.println(d.travelType.getName() + " dijkstra time: " + d.dijkstraDuration + "ms");
			});
		
		
		List<Route> routes = dijkstras.values().stream().map(dijkstra -> dijkstra.getPath(destinationNode)).collect(Collectors.toList());

		map.removeAllMapPolygons();
		
		routes.forEach(this::drawPath);

		dijkstraMutex.release(4);
		return true;

	}

	private void drawPath(Route route) {
		
		IntList path = route.path;

		for (int i = 1; i < path.size(); i++) {

			int from = path.getInt(i - 1);
			int to = path.getInt(i);

			Coordinate a = new Coordinate(graph.latOf(from), graph.lonOf(from));
			Coordinate b = new Coordinate(graph.latOf(to), graph.lonOf(to));

			MapPolygonImpl routPoly = new MapPolygonImpl("", a, b, b);
			routPoly.setColor(route.travelType.getColor());
		
//			if(i % 10 == 0) {
//				routPoly.setName(""+route.edgeSpeeds.getByte(i-1));
//			}
			// distribute name to random position
			if(i == route.path.size() / 2) {
				String name = route.travelType.getName() + ": ";
				name+= String.format("%.4f", route.totalDistance());
				routPoly.setName(name);
			}
			
			map.addMapPolygon(routPoly);

		}
	}

	@Override
	public void mousePressed(MouseEvent e) {
		if (e.getButton() == movementMouseButton || isPlatformOsx()
				&& e.getModifiersEx() == MAC_MOUSE_BUTTON3_MASK) {
			lastDragPoint = null;
			isMoving = true;
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (e.getButton() == movementMouseButton || isPlatformOsx()
				&& e.getButton() == MouseEvent.BUTTON1) {
			lastDragPoint = null;
			isMoving = false;
		}
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		if (wheelZoomEnabled) {
			map.setZoom(map.getZoom() - e.getWheelRotation(), e.getPoint());
		}
	}

	public boolean isMovementEnabled() {
		return movementEnabled;
	}

	/**
	 * Enables or disables that the map pane can be moved using the mouse.
	 *
	 * @param movementEnabled
	 *            {@code true} to allow the map pane to be moved using the mouse
	 */
	public void setMovementEnabled(boolean movementEnabled) {
		this.movementEnabled = movementEnabled;
	}

	public int getMovementMouseButton() {
		return movementMouseButton;
	}

	/**
	 * Sets the mouse button that is used for moving the map. Possible values
	 * are:
	 * <ul>
	 * <li>{@link MouseEvent#BUTTON1} (left mouse button)</li>
	 * <li>{@link MouseEvent#BUTTON2} (middle mouse button)</li>
	 * <li>{@link MouseEvent#BUTTON3} (right mouse button)</li>
	 * </ul>
	 *
	 * @param movementMouseButton
	 *            the mouse button that is used for moving the map
	 */
	public void setMovementMouseButton(int movementMouseButton) {
		this.movementMouseButton = movementMouseButton;
		switch (movementMouseButton) {
		case MouseEvent.BUTTON1:
			movementMouseButtonMask = MouseEvent.BUTTON1_DOWN_MASK;
			break;
		case MouseEvent.BUTTON2:
			movementMouseButtonMask = MouseEvent.BUTTON2_DOWN_MASK;
			break;
		case MouseEvent.BUTTON3:
			movementMouseButtonMask = MouseEvent.BUTTON3_DOWN_MASK;
			break;
		default:
			throw new RuntimeException("Unsupported button");
		}
	}

	public boolean isWheelZoomEnabled() {
		return wheelZoomEnabled;
	}

	public void setWheelZoomEnabled(boolean wheelZoomEnabled) {
		this.wheelZoomEnabled = wheelZoomEnabled;
	}

	public boolean isDoubleClickZoomEnabled() {
		return doubleClickZoomEnabled;
	}

	public void setDoubleClickZoomEnabled(boolean doubleClickZoomEnabled) {
		this.doubleClickZoomEnabled = doubleClickZoomEnabled;
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		// Mac OSX simulates with ctrl + mouse 1 the second mouse button hence
		// no dragging events get fired.
		//
		if (isPlatformOsx()) {
			if (!movementEnabled || !isMoving)
				return;
			// Is only the selected mouse button pressed?
			if (e.getModifiersEx() == MouseEvent.CTRL_DOWN_MASK) {
				Point p = e.getPoint();
				if (lastDragPoint != null) {
					int diffx = lastDragPoint.x - p.x;
					int diffy = lastDragPoint.y - p.y;
					map.moveMap(diffx, diffy);
				}
				lastDragPoint = p;
			}

		}

	}

	/**
	 * Replies true if we are currently running on OSX
	 *
	 * @return true if we are currently running on OSX
	 */
	public static boolean isPlatformOsx() {
		String os = System.getProperty("os.name");
		return os != null && os.toLowerCase().startsWith("mac os x");
	}
}
