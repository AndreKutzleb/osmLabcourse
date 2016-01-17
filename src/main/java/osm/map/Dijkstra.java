package osm.map;

import it.unimi.dsi.fastutil.ints.Int2IntAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.IntComparator;
import it.unimi.dsi.fastutil.ints.IntHeapPriorityQueue;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

public class Dijkstra {
	
	final Graph graph;
	
	public Dijkstra(Graph graph) {
		this.graph = graph;
		
	}
	
	
	public void findShortestPath(int fromNode, float toLat, float toLon) {
	

		// evtl bloomfilter
		final IntSet visited = new IntOpenHashSet();
		final Int2IntMap distanceToStart = new Int2IntAVLTreeMap();
		
		IntComparator dijkstraComparator = new IntComparator() {
			
			@Override
			public int compare(Integer k1, Integer k2) {
				int k1Dist = distanceToStart.get(k1);
				int k2Dist = distanceToStart.get(k2);
				return Integer.compare(k1Dist, k2Dist);
			}
			
			@Override
			public int compare(int k1, int k2) {
				int k1Dist = distanceToStart.get(k1);
				int k2Dist = distanceToStart.get(k2);
				return Integer.compare(k1Dist, k2Dist);
			}
		};
		
		IntHeapPriorityQueue queue = new IntHeapPriorityQueue(dijkstraComparator);
		
		queue.enqueue(fromNode);
		distanceToStart.put(fromNode, 0);
		
		while(!queue.isEmpty()) {
			int next = queue.dequeueInt();
			visited.add(next);
			int distanceToVisited = distanceToStart.get(next);
			
			graph.forEachNeighbourOf(next, (neighbour) -> {
				if(!visited.contains(neighbour)) {
					// See if we can get there faster
					int distanceToStartOfNeighbour = distanceToStart.get(neighbour);
				}
			});
		}
	}

}
