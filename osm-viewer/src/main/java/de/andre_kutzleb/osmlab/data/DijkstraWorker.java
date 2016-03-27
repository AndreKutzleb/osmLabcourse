package de.andre_kutzleb.osmlab.data;

import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import javax.swing.SwingWorker;

import osm.map.Dijkstra;
import osm.map.Route;

public class DijkstraWorker extends SwingWorker<Integer, String> {

	private final Dijkstra dijkstra;
	private int startNode;
	private final Semaphore dijkstraMutex;
	private int populationMultiplier;
	private boolean preferPopulation;
	private final AtomicInteger currentDestination;
	private final Consumer<Route> pathDrawer;

	public DijkstraWorker(Dijkstra dikjstra, int startNode,
			Semaphore dijkstraMutex, int populationMultiplier, boolean preferPopulation, AtomicInteger currentDestination, Consumer<Route> pathDrawer) {
		this.dijkstra = dikjstra;
		this.startNode = startNode;
		this.dijkstraMutex = dijkstraMutex;
		this.populationMultiplier = populationMultiplier;
		this.preferPopulation = preferPopulation;
		this.currentDestination = currentDestination;
		this.pathDrawer = pathDrawer;
	}

	@Override
	protected Integer doInBackground() throws Exception {

		try {
			Thread.currentThread().setName("DijkstraWorker " + this.dijkstra.getName());
			dijkstra.precalculateDijkstra(startNode, this::setProgress,populationMultiplier, preferPopulation, currentDestination, pathDrawer,dijkstraMutex);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			dijkstraMutex.release();
		}

		return 100;
	}

	@Override
	protected void process(List<String> chunks) {
		// TODO Auto-generated method stub
		super.process(chunks);
	}

}
