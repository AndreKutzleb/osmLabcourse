package osm.map;

import java.util.Random;
import java.util.function.IntConsumer;

public class GraphClickFinder {
	
	private static final int numberOfRandomStartNodes = 50000;

	private final Graph graph;
	private final Random rand = new Random(0);
	
	public GraphClickFinder(Graph graph) {
		this.graph = graph;
	}

	public int findClosestNodeTo(float toLat, float toLon) {
		int closestStartNode = findClosestStartNode(toLat,toLon);
		return new Dijkstra(graph).findClosestNodeDijkstra(closestStartNode, toLat, toLon);
	}

	private int findClosestStartNode(float toLat, float toLon) {
		int minDistNode = 0;
		float minDist = Integer.MAX_VALUE;
		
		for(int i = 0; i < numberOfRandomStartNodes; i++) {
			int randomNode = rand.nextInt(graph.getNodeCount());
			float distance = graph.distance(randomNode, toLat, toLon);
			
			if(distance < minDist) {
				minDist = distance;
				minDistNode = randomNode;
			}
			
		}
		return minDistNode;
	}
	
	

}
