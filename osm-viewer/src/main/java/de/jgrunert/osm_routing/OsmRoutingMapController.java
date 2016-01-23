// License: GPL. For details, see Readme.txt file.
package de.jgrunert.osm_routing;

import it.unimi.dsi.fastutil.ints.IntList;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Point;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.beans.PropertyChangeListener;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;

import javax.swing.JProgressBar;
import javax.swing.SwingWorker.StateValue;

import net.sf.geographiclib.Geodesic;
import net.sf.geographiclib.GeodesicData;

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

	private int startIndex;
	private Coordinate startLoc = null;
	private int targetIndex;
	private Coordinate targetLoc = null;

	private List<MapMarkerDot> routeDots = new ArrayList<>();
	private List<MapPolygonImpl> routeLines = new ArrayList<>();

	int nodeCount = 0;
	double[] nodesLat = null;
	double[] nodesLon = null;
	int[] nodesEdgeOffset = null;

	int[] nodesPreBuffer = null;

	int edgeCount = 0;
	int[] edgesTarget = null;

	private final Graph graph;
	private final Dijkstra dijkstraPedestrian;
	private final Dijkstra dijkstraCarShortest;
	private final Dijkstra dijkstraCarFastest;
	private JProgressBar ped;
	private JProgressBar carS;
	private JProgressBar carF;

	public OsmRoutingMapController(JMapViewer map, JProgressBar ped,
			JProgressBar carS, JProgressBar carF) {
		super(map);
		this.ped = ped;
		this.carS = carS;
		this.carF = carF;

		Graph graph = null;
		try {
			// graph = Graph.createGraph("saarland-latest.osm.pbf");
			graph = Graph.createGraph("germany-latest.osm.pbf");

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		this.graph = graph;
		this.dijkstraPedestrian = new Dijkstra(graph, TravelType.PEDESTRIAN);
		this.dijkstraCarShortest = new Dijkstra(graph, TravelType.CAR_SHORTEST);
		this.dijkstraCarFastest = new Dijkstra(graph, TravelType.CAR_FASTEST);

		//
		// try {
		// loadOsmData();
		// } catch (Exception e) {
		// System.err.println("Error at loadOsmData");
		// e.printStackTrace();
		// }
	}

	@SuppressWarnings("resource")
	private void loadOsmData() throws Exception {

		System.out.println("Start reading nodes");
		DataInputStream nodeReader = new DataInputStream(new FileInputStream(
				"D:\\Jonas\\OSM\\hamburg\\pass3-nodes.bin"));

		nodeCount = nodeReader.readInt();
		nodesLat = new double[nodeCount];
		nodesLon = new double[nodeCount];
		nodesEdgeOffset = new int[nodeCount];
		nodesPreBuffer = new int[nodeCount];

		for (int i = 0; i < nodeCount; i++) {
			nodesLat[i] = nodeReader.readDouble();
			nodesLon[i] = nodeReader.readDouble();
			nodesEdgeOffset[i] = nodeReader.readInt();
		}

		nodeReader.close();
		System.out.println("Finished reading nodes");

		System.out.println("Start reading edges");
		DataInputStream edgeReader = new DataInputStream(new FileInputStream(
				"D:\\Jonas\\OSM\\hamburg\\pass3-edges.bin"));
		edgeCount = edgeReader.readInt();
		edgesTarget = new int[edgeCount];

		for (int i = 0; i < edgeCount; i++) {
			edgesTarget[i] = edgeReader.readInt();
			if (edgesTarget[i] == 0) {
				System.out.println(i);
			}
		}

		edgeReader.close();
		System.out.println("Finished reading edges");

		// for(int i = 4; i < 5; i++) {
		// double lat = nodesLat[i];
		// double lon = nodesLon[i];
		// Coordinate coord = new Coordinate(lat, lon);
		// int edgeOffs = nodesEdgeOffset[i];
		// MapMarkerDot targetDot = new MapMarkerDot("Start", coord);
		// map.addMapMarker(targetDot);
		// Set<Integer> visited = new HashSet<>();
		// visited.add(i);
		//
		// for(int iTarg = edgeOffs; iTarg < nodesEdgeOffset[i+1]; iTarg++) {
		// mapPointDfs(edgesTarget[iTarg],coord, 1, 300, visited);
		// }
		// }
	}

	private void mapPointDfs(int i, Coordinate lastCoord, int depth,
			int maxDepth, Set<Integer> visited) {
		if (depth < maxDepth) {
			double lat = nodesLat[i];
			double lon = nodesLon[i];
			Coordinate coord = new Coordinate(lat, lon);
			int edgeOffs = nodesEdgeOffset[i];

			if (depth == maxDepth - 1) {
				// MapMarkerDot targetDot = new MapMarkerDot("End", coord);
				// map.addMapMarker(targetDot);
			}

			MapPolygonImpl routPoly = new MapPolygonImpl(lastCoord, coord,
					coord);
			routeLines.add(routPoly);
			map.addMapPolygon(routPoly);
			visited.add(i);

			boolean hasWay = false;
			for (int iTarg = edgeOffs; iTarg < nodesEdgeOffset[i + 1]; iTarg++) {
				int targ = edgesTarget[iTarg];
				if (!visited.contains(targ)) {
					hasWay = true;
					mapPointDfs(edgesTarget[iTarg], coord, depth + 1, maxDepth,
							visited);
					// break;
				} else {
					System.out.println("Already visited");
				}
			}

			if (!hasWay) {
				// MapMarkerDot targetDot = new MapMarkerDot("Sack", coord);
				// map.addMapMarker(targetDot);
			}
		}
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
					(float) clickPt.getLat(), (float) clickPt.getLon());

			// graph.forEachNeighbourOf(clickNextPt, neighbour -> {
			// Coordinate c = new
			// Coordinate(graph.latOf(neighbour),graph.lonOf(neighbour));
			// MapMarkerDot cl = new MapMarkerDot("Neighbour"+neighbour, c);
			// map.addMapMarker(cl);
			//
			//
			// },3);

			// graph.forEachEdgeOf(clickNextPt, (from,to) -> {
			// Coordinate a = new Coordinate(graph.latOf(from),
			// graph.lonOf(from));
			// Coordinate b = new Coordinate(graph.latOf(to), graph.lonOf(to));
			//
			// MapPolygonImpl poly = new MapPolygonImpl(a,b,b);
			// map.addMapPolygon(poly);
			// },10);

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
			if (startDot != null && stopDot != null) {
				System.out.println("starting do dijkstra yo");
				IntList findPathDijkstra;
				long before = System.currentTimeMillis();
				// findPathDijkstra = dijkstra.findPathDijkstra(startNode,
				// stopNode);
				long middle = System.currentTimeMillis();
				findPathDijkstra = null;// dijkstra.findPathDijkstraFast(startNode,
										// stopNode);
				long after = System.currentTimeMillis();

				System.out.println("normal: " + (middle - before) + "ms");
				System.out.println("fast:" + (after - middle) + "ms");
				map.removeAllMapPolygons();
				for (int i = 1; i < findPathDijkstra.size(); i++) {

					int from = findPathDijkstra.getInt(i - 1);
					int to = findPathDijkstra.getInt(i);

					Coordinate a = new Coordinate(graph.latOf(from),
							graph.lonOf(from));
					Coordinate b = new Coordinate(graph.latOf(to),
							graph.lonOf(to));

					MapPolygonImpl routPoly = new MapPolygonImpl("", a, b, b);
					map.addMapPolygon(routPoly);
				}
			}
			//
			// Coordinate clickNextPtCoord = new
			// Coordinate(nodesLat[clickNextPt], nodesLon[clickNextPt]);
			//
			// if(e.getButton() == MouseEvent.BUTTON1) {
			// startIndex = clickNextPt;
			// startLoc = clickNextPtCoord;
			// }
			// else if(e.getButton() == MouseEvent.BUTTON3) {
			// targetIndex = clickNextPt;
			// targetLoc = clickNextPtCoord;
			// }
			//
			// updateRoute();
		} else if (doubleClickZoomEnabled && e.getClickCount() == 2
				&& e.getButton() == MouseEvent.BUTTON1) {
			// Zoom on doubleclick
			map.zoomIn(e.getPoint());
		}
	}

	// 1 needed to set new target, 2 needed to move destination.
	// after first successful run of dijkstra from a start Node and
	// from then on out, semaphore has 2 permits

	private final Semaphore dijkstraMutex = new Semaphore(3);
	boolean canRoute = false;

	volatile DijkstraWorker pedestrianRef = null;
	volatile DijkstraWorker carShortestRef = null;
	volatile DijkstraWorker carFastestRef = null;

	private void onLeftClick(int startNode) throws InterruptedException {

		boolean canCalculate = dijkstraMutex.tryAcquire(3);

		if (!canCalculate) {
			// currently running calculation. stop it.
			pedestrianRef.cancel(true);
			carShortestRef.cancel(true);
			carFastestRef.cancel(true);

			// will wait for the old tasks to stop
			dijkstraMutex.acquire(3);
		}
		canRoute = false;

		final DijkstraWorker pedestrian = new DijkstraWorker(
				dijkstraPedestrian, startNode, dijkstraMutex);
		final DijkstraWorker carShortest = new DijkstraWorker(
				dijkstraCarShortest, startNode, dijkstraMutex);
		final DijkstraWorker carFastest = new DijkstraWorker(
				dijkstraCarFastest, startNode, dijkstraMutex);

		pedestrianRef = pedestrian;
		carShortestRef = carShortest;
		carFastestRef = carFastest;

		ped.setVisible(true);
		carS.setVisible(true);
		carF.setVisible(true);

		PropertyChangeListener p = (c) -> {
			ped.setValue(pedestrian.getProgress());
			carS.setValue(carShortest.getProgress());
			carF.setValue(carFastest.getProgress());

			ped.setString("Pedestrian: " + pedestrian.getProgress() + "%");
			carS.setString("Car Shortest: " + carShortest.getProgress() + "%");
			carF.setString("Car Fastest: " + carFastest.getProgress() + "%");

			float avgPercent = (pedestrian.getProgress()
					+ carShortest.getProgress() + carFastest.getProgress()) / 300f;
			// draw temporary direct air line
			if (stopDot != null) {
				drawTempLine(startNode, stopDotNode, avgPercent);
			}

			boolean stop = pedestrian.isDone() && carShortest.isDone()
					&& carFastest.isDone();

			boolean cancel = pedestrian.isCancelled()
					|| carShortest.isCancelled() || carFastest.isCancelled();

			if (stop) {
				canRoute = true;
			}

			if (stopDot != null && !cancel) {
				// in case there is already a destination, 
				//instantly show the path
				onRightClick(stopDotNode);
			}

		};

		pedestrian.addPropertyChangeListener(p);
		carShortest.addPropertyChangeListener(p);
		carFastest.addPropertyChangeListener(p);
		pedestrian.execute();
		carShortest.execute();
		carFastest.execute();

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

		boolean gotLock = dijkstraMutex.tryAcquire(3);

		if (!gotLock) {
			return false;
		}

		if (!canRoute) {
			dijkstraMutex.release(3);
			return false;
		}
		IntList pedestrianPath = dijkstraPedestrian.getPath(destinationNode);
		IntList carShortestPath = dijkstraCarShortest.getPath(destinationNode);
		IntList carFastestPath = dijkstraCarFastest.getPath(destinationNode);

		map.removeAllMapPolygons();

		drawPath(pedestrianPath, Color.GREEN);
		drawPath(carShortestPath, Color.RED);
		drawPath(carFastestPath, Color.BLUE);

		dijkstraMutex.release(3);
		return true;

	}

	private void drawPath(IntList path, Color color) {
		for (int i = 1; i < path.size(); i++) {

			int from = path.getInt(i - 1);
			int to = path.getInt(i);

			Coordinate a = new Coordinate(graph.latOf(from), graph.lonOf(from));
			Coordinate b = new Coordinate(graph.latOf(to), graph.lonOf(to));

			MapPolygonImpl routPoly = new MapPolygonImpl("", a, b, b);
			routPoly.setColor(color);
			map.addMapPolygon(routPoly);
		}
	}

	/**
	 * Tries to find out index of next point to given coordinate
	 * 
	 * @param coord
	 * @return Index of next point
	 */
	private int findNextPoint(Coordinate coord) {
		int nextIndex = -1;
		double smallestDist = Double.MAX_VALUE;
		for (int i = 0; i < nodeCount; i++) {
			GeodesicData g = Geodesic.WGS84.Inverse(coord.getLat(),
					coord.getLon(), nodesLat[i], nodesLon[i]);
			if (g.s12 < smallestDist) {
				smallestDist = g.s12;
				nextIndex = i;
			}
		}
		return nextIndex;
	}

	private void updateRoute() {
		// Remove old dots and lines
		for (MapMarkerDot dot : routeDots) {
			map.removeMapMarker(dot);
		}
		routeDots.clear();
		for (MapPolygonImpl line : routeLines) {
			map.removeMapPolygon(line);
		}
		routeLines.clear();

		// Add route points to map
		if (startLoc != null) {
			MapMarkerDot startDot = new MapMarkerDot("Start", startLoc);
			map.addMapMarker(startDot);
			routeDots.add(startDot);
		}
		if (targetLoc != null) {
			MapMarkerDot targetDot = new MapMarkerDot("Target", targetLoc);
			map.addMapMarker(targetDot);
			routeDots.add(targetDot);
		}

		// if(startLoc != null && targetLoc != null) {
		// MapPolygonImpl routPoly = new MapPolygonImpl("Route", startLoc,
		// targetLoc, targetLoc);
		// routeLines.add(routPoly);
		// map.addMapPolygon(routPoly);
		// }

		Random rd = new Random(123);
		double debugDispProp = 10.998;

		if (startLoc != null && targetLoc != null) {
			// BFS uses Queue data structure
			Queue<Integer> queue = new LinkedList<>();
			Set<Integer> visited = new HashSet<>();
			queue.add(startIndex);
			while (!queue.isEmpty()) {
				int nextIndex = queue.remove();

				if (nextIndex == targetIndex) {
					System.out.println("Found!");
					break;
				} else {
					// System.out.println("Not found");
				}

				visited.add(nextIndex);

				// Display
				if (rd.nextDouble() > debugDispProp) {
					MapMarkerDot targetDot = new MapMarkerDot(new Coordinate(
							nodesLat[nextIndex], nodesLon[nextIndex]));
					map.addMapMarker(targetDot);
					routeDots.add(targetDot);
				}
				// System.out.println(nextIndex);

				for (int iTarg = nodesEdgeOffset[nextIndex];
				// nextIndex+1 < nodesEdgeOffset.length && TODO last node
				iTarg < nodesEdgeOffset[nextIndex + 1]; iTarg++) {
					int targ = edgesTarget[iTarg];
					if (!visited.contains(targ)) {
						nodesPreBuffer[targ] = nextIndex;
						queue.add(targ);
					} else {
						// System.out.println("Already visited");
					}
				}
			}

			int i = targetIndex;
			while (i != startIndex) {
				int pre = nodesPreBuffer[i];
				// if (pre == targetIndex) {
				// continue;
				// }

				Coordinate c1 = new Coordinate(nodesLat[pre], nodesLon[pre]);
				Coordinate c2 = new Coordinate(nodesLat[i], nodesLon[i]);

				MapPolygonImpl routPoly = new MapPolygonImpl(c1, c2, c2);
				routeLines.add(routPoly);
				map.addMapPolygon(routPoly);

				// MapMarkerDot dot = new MapMarkerDot(new
				// Coordinate(nodesLat[i], nodesLon[i]));
				// map.addMapMarker(dot);
				// routeDots.add(dot);

				i = pre;
			}
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
