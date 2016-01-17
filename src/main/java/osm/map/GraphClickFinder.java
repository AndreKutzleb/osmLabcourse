package osm.map;

import java.util.Random;

public class GraphClickFinder {
	
	private static final int numberOfRandomStartNodes = 1000;

	private final Graph graph;
	private final Random rand = new Random(0);
	
	public GraphClickFinder(Graph graph) {
		this.graph = graph;
	}

	public int findClosestNodeTo(float toLat, float toLon) {
		int closestStartNode = findClosestStartNode(toLat,toLon);
	}

	private int findClosestStartNode(float toLat, float toLon) {
		int minDistNode = 0;
		float minDist = Integer.MAX_VALUE;
		
		for(int i = 0; i < numberOfRandomStartNodes; i++) {
			int randomNode = rand.nextInt(graph.getNodeCount());
			float distance = graph.distance(randomNode, toLat, toLon);
			
			if(distance < minDist) {
				distance = minDist;
				minDistNode = randomNode;
			}
			
		}
		
		return minDistNode;
	}
	
	

}
