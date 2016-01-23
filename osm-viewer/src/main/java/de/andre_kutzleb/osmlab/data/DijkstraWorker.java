package de.andre_kutzleb.osmlab.data;

import java.util.List;

import javax.swing.SwingWorker;

import osm.map.Dijkstra;

public class DijkstraWorker extends SwingWorker<Integer, String>{
	
	private final Dijkstra dijkstra;
	
	public DijkstraWorker(Dijkstra dikjstra) {
		this.dijkstra = dikjstra;
	}
	
	  @Override
	  protected Integer doInBackground() throws Exception {
		  
	    // Start
	    publish("Start");
	    setProgress(1);
	    
	    // More work was done
	    publish("More work was done");
	    setProgress(10);

	    // Complete
	    publish("Complete");
	    setProgress(100);
	    return 1;
	  }
	  
	  @Override
	protected void process(List<String> chunks) {
		// TODO Auto-generated method stub
		super.process(chunks);
	}


}
