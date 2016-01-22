package osm.map;

import java.util.Arrays;
import java.util.BitSet;

import osmlab.sink.GeoUtils;
import osmlab.sink.GeoUtils.FloatPoint;
import it.unimi.dsi.fastutil.ints.Int2IntAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntComparator;
import it.unimi.dsi.fastutil.ints.IntHeapIndirectPriorityQueue;
import it.unimi.dsi.fastutil.ints.IntHeapPriorityQueue;
import it.unimi.dsi.fastutil.ints.IntHeapSemiIndirectPriorityQueue;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

public class Dijkstra {

	final Graph graph;
	final int[] refArray;
	final int[] successor;
	final boolean[] visited;
	final IntHeapIndirectPriorityQueue queue;

	private void resetData() {
		Arrays.fill(refArray, 0);
		Arrays.fill(visited, false);
		queue.clear();
	}

	int latestSource = 0;

	public Dijkstra(Graph graph) {
		this.graph = graph;
		refArray = new int[graph.getNodeCount()];
		successor = new int[graph.getNodeCount()];
		visited = new boolean[graph.getNodeCount()];
		queue = new IntHeapIndirectPriorityQueue(refArray);
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

	public IntList findPathDijkstra(int fromNode, int toNode) {

		FloatPoint middle = GeoUtils
				.midPoint(graph.latOf(fromNode), graph.lonOf(fromNode),
						graph.latOf(toNode), graph.lonOf(toNode));
		float maxRadius = GeoUtils.distFrom(middle.lat, middle.lon,
				graph.latOf(fromNode), graph.lonOf(fromNode)) * 1.2f;

		final IntSet visited = new IntOpenHashSet();
		final Int2IntMap distanceToStart = new Int2IntAVLTreeMap();
		final Int2IntMap successor = new Int2IntAVLTreeMap();

		distanceToStart.defaultReturnValue(-1);

		IntHeapPriorityQueue queue = new IntHeapPriorityQueue(
				getDijkstraComparator(distanceToStart));

		queue.enqueue(fromNode);
		distanceToStart.put(fromNode, 0);

		while (!queue.isEmpty()) {
			// if(queue.size() % 1000 == 0) {
			// System.out.println(queue.size());
			// System.out.println("queueSize " + visited.size());
			// }
			int next = queue.dequeueInt();
			if (visited.contains(next)) {
				continue;
			}
			visited.add(next);
			int distanceToVisited = distanceToStart.get(next);

			if (next == toNode) {
				break;
			}

			// float geoDistToClick = graph.distance(next, toLat, toLon);
			// if(geoDistToClick < shortestFoundDistance) {
			// shortestFoundDistance = geoDistToClick;
			// shortestFoundDistanceNode = next;
			// }

			graph.forEachNeighbourOf(
					next,
					(neighbour) -> {
						if (!visited.contains(neighbour)
								&& graph.distance(neighbour, middle.lat,
										middle.lon) < maxRadius) {
							// See if we can get there faster
							int distanceToStartOfNeighbour = distanceToStart
									.get(neighbour);
							int distanceFromNext = distanceToVisited + 1;

							if (distanceFromNext < distanceToStartOfNeighbour
									|| distanceToStartOfNeighbour == -1) {
								// can get there faster
								distanceToStart
										.put(neighbour, distanceFromNext);
								successor.put(neighbour, next);
							}
							// may cause multiple instances of ways to
							// same node, but closest way will come first
							queue.enqueue(neighbour);
						}
					});
		}

		IntList path = new IntArrayList();

		int current = toNode;
		path.add(toNode);
		while ((current = successor.get(current)) != fromNode) {
			path.add(current);
		}
		path.add(fromNode);
		// TODO reverse
		return path;
	}

	public IntList findPathDijkstraFast(int fromNode, int toNode) {

		if (latestSource != fromNode) {
			resetData();
			latestSource = fromNode;
		}

		FloatPoint middle = GeoUtils
				.midPoint(graph.latOf(fromNode), graph.lonOf(fromNode),
						graph.latOf(toNode), graph.lonOf(toNode));
		float maxRadius = GeoUtils.distFrom(middle.lat, middle.lon,
				graph.latOf(fromNode), graph.lonOf(fromNode)) * 1.2f;

		queue.enqueue(fromNode);

		while (!queue.isEmpty()) {
			// if(queue.size() % 1000 == 0) {
			// System.out.println(queue.size());
			// System.out.println("queueSize " + visited.size());
			// }
			int next = queue.dequeue();

			if (next == toNode) {
				break;
			}

			if (visited[next]) {
				continue;
			} else {
				visited[next] = true;
			}

			int distanceToVisited = refArray[next];

			// float geoDistToClick = graph.distance(next, toLat, toLon);
			// if(geoDistToClick < shortestFoundDistance) {
			// shortestFoundDistance = geoDistToClick;
			// shortestFoundDistanceNode = next;
			// }

			graph.forEachNeighbourOf(
					next,
					(neighbour) -> {

						if (!visited[neighbour]) {

							if (queue.contains(neighbour)) {

								int distanceToStartOfNeighbour = refArray[neighbour];
								int distanceFromNext = distanceToVisited + 1;

								boolean improvement = distanceFromNext < distanceToStartOfNeighbour
										|| distanceToStartOfNeighbour == 0;

								if (improvement) {
									// can get there faster
									refArray[neighbour] = distanceFromNext;
									successor[neighbour] = next;
									queue.changed(neighbour);
								}
							}

							else /*
								 * if (graph.distanceFast(neighbour, middle.lat,
								 * middle.lon) < maxRadius)
								 */{
								// fastforward
								int distanceFromNext = distanceToVisited + 1;

								int beforeNeighbour = next;
								int currNeighbour = neighbour;
								// while(true)
								int nextNeighbour = graph.neighbourOf(
										currNeighbour, beforeNeighbour);
								while (graph.neighbourCount(currNeighbour) == 2
										&& !visited[nextNeighbour]) {
									visited[currNeighbour] = true;
									successor[currNeighbour] = beforeNeighbour;
									beforeNeighbour = currNeighbour;
									currNeighbour = nextNeighbour;
									nextNeighbour = graph.neighbourOf(
											currNeighbour, beforeNeighbour);
									distanceFromNext++;
									if (currNeighbour == toNode) {
										break;
									}
								}

								if (queue.contains(currNeighbour)) {

									int distanceToStartOfNeighbour = refArray[currNeighbour];

									boolean improvement = distanceFromNext < distanceToStartOfNeighbour
											|| distanceToStartOfNeighbour == 0;

									if (improvement) {
										// can get there faster
										refArray[currNeighbour] = distanceFromNext;
										successor[currNeighbour] = beforeNeighbour;
										queue.changed(currNeighbour);
									}

								} else {
									refArray[currNeighbour] = distanceFromNext;
									successor[currNeighbour] = beforeNeighbour;;
									queue.enqueue(currNeighbour);
								}
							}
						}

					});
		}

		IntList path = new IntArrayList();

		int current = toNode;
		path.add(toNode);
		while ((current = successor[current]) != fromNode) {
			if (current == successor[current]) {
				throw new IllegalStateException("invalid path: " + path + " + "
						+ current);
			}
			path.add(current);
		}
		path.add(fromNode);
		// TODO reverse
		return path;
	}

}
