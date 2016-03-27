package de.andre_kutzleb.osmlab.data;

import java.util.List;
import java.util.concurrent.Semaphore;

import javax.swing.SwingWorker;

import osm.map.Dijkstra;

public class DijkstraWorker extends SwingWorker<Integer, String> {

	private final Dijkstra dijkstra;
	private int startNode;
	private final Semaphore dijkstraMutex;
	private int populationMultiplier;
	private boolean preferPopulation;

	public DijkstraWorker(Dijkstra dikjstra, int startNode,
			Semaphore dijkstraMutex, int populationMultiplier, boolean preferPopulation) {
		this.dijkstra = dikjstra;
		this.startNode = startNode;
		this.dijkstraMutex = dijkstraMutex;
		this.populationMultiplier = populationMultiplier;
		this.preferPopulation = preferPopulation;
	}

	@Override
	protected Integer doInBackground() throws Exception {

		try {
			Thread.currentThread().setName("DijkstraWorker " + this.dijkstra.getName());
			dijkstra.precalculateDijkstra(startNode, this::setProgress,populationMultiplier, preferPopulation);
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
