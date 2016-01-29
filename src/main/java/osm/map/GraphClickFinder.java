package osm.map;

import it.unimi.dsi.fastutil.ints.Int2IntAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.IntComparator;
import it.unimi.dsi.fastutil.ints.IntHeapPriorityQueue;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.util.Arrays;
import java.util.Random;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.ObjIntConsumer;
import java.util.stream.IntStream;

public class GraphClickFinder {
	
	private static final int numberOfRandomStartNodes = 500000;
	private static final int CANNOT_LOOK_FOR_NODE_LIMIT = 100 * 1000;

	private final Graph graph;
	private final Random rand = new Random(0);
	
	public GraphClickFinder(Graph graph) {
		this.graph = graph;
	}

	public int findClosestNodeTo(float toLat, float toLon, boolean deterministic) {
		if(deterministic) {
//			long before = System.currentTimeMillis();
//			int found1 = findDeterministic(toLat, toLon);
//			long middle = System.currentTimeMillis();
			int found2 = findDeterministicFast(toLat, toLon);
//			long after = System.currentTimeMillis();
//			
//			System.out.print("normal: ");
//			System.out.println(middle-before);
//			System.out.print("parallel: ");
//			System.out.println(after-middle);
//			
//			if(found1 != found2) {
//				throw new IllegalStateException();
//			}
			return found2;

		} else {
			
		int closestStartNode = findClosestStartNode(toLat,toLon);
		if(graph.distance(closestStartNode, toLat, toLon) > CANNOT_LOOK_FOR_NODE_LIMIT) {
			System.err.println("Returning closestStartNode");
			return closestStartNode;
		}
		return findClosestNodeDijkstra(closestStartNode, toLat, toLon);
		}
	}
	
	private int findDeterministic(float toLat, float toLon) {
		int closest = 0;
		float closestDistance = graph.distance(0, toLat, toLon);
		
		for(int node = 1; node <  graph.getNodeCount(); node++) {
			float distance = graph.distance(node, toLat, toLon);
			if(distance < closestDistance) {
				closest = node;
				closestDistance = distance;
			}
		}
		return closest;
	}
	
	
	private class Min{
		int node = 0;
		float distance = Integer.MAX_VALUE;
	}
	
	private int findDeterministicFast(float toLat, float toLon) {
		
		ObjIntConsumer<Min> accumulator = new ObjIntConsumer<Min>() {	
				
			@Override
			public void accept(Min arg0, int arg1) {
				float dist = graph.distance(arg1,toLat,toLon);
				if(dist < arg0.distance) {
					arg0.distance = dist;
					arg0.node = arg1;
				}
			}
		};
		
		BiConsumer<Min, Min> combiner = new BiConsumer<Min, Min>() {
			
			@Override
			public void accept(Min t, Min u) {
				if(u.distance < t.distance) {
					t.distance = u.distance;
					t.node = u.node;
				}
			}
		};
			
		Min min = IntStream.range(0, graph.getNodeCount()).parallel().collect(Min::new, accumulator, combiner);
	
		return min.node;
	}

	public static IntComparator getDijkstraComparator(Int2IntMap distanceToStart) {
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

		return dijkstraComparator;
	}
	
	
	public int findClosestNodeDijkstra(int fromNode, float toLat, float toLon) {

		float distanceFromClick = graph.distance(fromNode, toLat, toLon);

		float shortestFoundDistance = Integer.MAX_VALUE;
		int shortestFoundDistanceNode = 0;

		// evtl bloomfilter
		final IntSet visited = new IntOpenHashSet();
		final Int2IntMap distanceToStart = new Int2IntAVLTreeMap();
		// final Int2IntMap successor = new Int2IntAVLTreeMap();
		distanceToStart.defaultReturnValue(-1);

		IntHeapPriorityQueue queue = new IntHeapPriorityQueue(
				getDijkstraComparator(distanceToStart));

		queue.enqueue(fromNode);
		distanceToStart.put(fromNode, 0);

		while (!queue.isEmpty()) {
			if (queue.size() % 1000 == 0) {
				System.out.println(queue.size());
				System.out.println("queueSize " + visited.size());
			}
			int next = queue.dequeueInt();
			visited.add(next);
			int distanceToVisited = distanceToStart.get(next);

			float geoDistToClick = graph.distance(next, toLat, toLon);
			if (geoDistToClick < shortestFoundDistance) {
				shortestFoundDistance = geoDistToClick;
				shortestFoundDistanceNode = next;
			}

			graph.forEachNeighbourOf(
					next,
					(neighbour) -> {
						if (!visited.contains(neighbour)
								&& graph.distance(neighbour, toLat, toLon) < (1.1 * distanceFromClick)) {
							// See if we can get there faster
							int distanceToStartOfNeighbour = distanceToStart
									.get(neighbour);
							int distanceFromNext = distanceToVisited + 1;

							if (distanceFromNext < distanceToStartOfNeighbour
									|| distanceToStartOfNeighbour == -1) {
								// can get there faster
								distanceToStart
										.put(neighbour, distanceFromNext);
								// successor.put(neighbour, next);
							}
							// may cause multiple instances of ways to
							// same node, but closest way will come first
							queue.enqueue(neighbour);
						}
					});

		}

		return shortestFoundDistanceNode;
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
