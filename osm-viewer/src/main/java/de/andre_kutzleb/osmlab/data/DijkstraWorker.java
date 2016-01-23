package de.andre_kutzleb.osmlab.data;

import java.util.List;
import java.util.concurrent.Semaphore;

import javax.swing.SwingWorker;

import osm.map.Dijkstra;

public class DijkstraWorker extends SwingWorker<Integer, String>{
	
	private final Dijkstra dijkstra;
	private int startNode;
	private final Semaphore dijkstraMutex;
	
	public DijkstraWorker(Dijkstra dikjstra, int startNode, Semaphore dijkstraMutex) {
		this.dijkstra = dikjstra;
		this.startNode = startNode;
		this.dijkstraMutex = dijkstraMutex;
	}
	
	  @Override
	  protected Integer doInBackground() throws Exception {
		  
		  dijkstra.precalculateDijkstra(startNode, this::setProgress);
		  dijkstraMutex.release();
//	    // Start
//	    publish("Start");
//	    setProgress(1);
//	    
//	    // More work was done
//	    publish("More work was done");
//	    setProgress(10);
//
//	    // Complete
//	    publish("Complete");
//	    setProgress(100);
	    return 100;
	  }
	  
	  @Override
	protected void process(List<String> chunks) {
		// TODO Auto-generated method stub
		super.process(chunks);
	}


}
