package de.andre_kutzleb.osm_routing;

import java.util.HashMap;
import java.util.Map;

import javax.swing.JLabel;
import javax.swing.JProgressBar;

import osm.map.Dijkstra;
import osm.map.Graph;

public class RoutingOptions {

	private final Map<String,Dijkstra> routingOptions = new HashMap<>();
	private final Map<String,JProgressBar> progress = new HashMap<>();
	private final Map<String,JLabel> routingInformation = new HashMap<>();
	public final Graph graph;
	
	public RoutingOptions(Graph graph) {
		this.graph = graph;
	}

	public void addRoutingOption(Dijkstra strategy, JProgressBar progress) {
		this.routingOptions.put(strategy.getName(), strategy);
		this.progress.put(strategy.getName(), progress);
		this.routingInformation.put(strategy.getName(), new JLabel(strategy.getName()));
	}
	
	public Map<String, JProgressBar> getProgress() {
		return progress;
	}
	
	public Map<String, Dijkstra> getRoutingOptions() {
		return routingOptions;
	}
	
	public Map<String, JLabel> getRoutingInformation() {
		return routingInformation;
	}

	public int size() {
		return routingOptions.size();
	}
	
}
