package osm.map;

import it.unimi.dsi.fastutil.ints.Int2IntAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntComparator;
import it.unimi.dsi.fastutil.ints.IntHeapIndirectPriorityQueue;
import it.unimi.dsi.fastutil.ints.IntHeapPriorityQueue;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.util.Arrays;
import java.util.function.IntConsumer;

import osmlab.sink.GeoUtils;
import osmlab.sink.GeoUtils.FloatPoint;

public class Dijkstra {
	
	public enum TravelType {
		PEDESTRIAN,
		CAR_SHORTEST,
		CAR_FASTEST
	}

	final Graph graph;
	final int[] refArray;
	final int[] successor;
	final boolean[] visited;
	public final TravelType travelType;
	int visitedCount = 0;
	final IntHeapIndirectPriorityQueue queue;

	private void resetData() {
		Arrays.fill(refArray, 0);
		Arrays.fill(visited, false);
		visitedCount = 0;
		queue.clear();
	}

	int latestSource = 0;
	private int fromNode = -1;

	public Dijkstra(Graph graph, TravelType travelType) {
		this.travelType = travelType;	
		this.graph = graph;
		refArray = new int[graph.getNodeCount()];
		successor = new int[graph.getNodeCount()];
		visited = new boolean[graph.getNodeCount()];
		queue = new IntHeapIndirectPriorityQueue(refArray,graph.getNodeCount());
	}

	public IntList getPath(int toNode) {

		
		IntList path = new IntArrayList();
		// no way possible.
		if(!visited[toNode]) {
			return path; 
		}

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
		return path;
	}

	public void precalculateDijkstra(int fromNode, IntConsumer progressConsumer) {

			resetData();
			this.fromNode = fromNode;

			queue.enqueue(fromNode);

			int currentProgress = 0;
			
			progressConsumer.accept(0);
		while (!queue.isEmpty()) {
			// if(queue.size() % 1000 == 0) {
			// System.out.println(queue.size());
			// System.out.println("queueSize " + visited.size());
			// }
			int next = queue.dequeue();
			
			int progress = (visitedCount) / (graph.getNodeCount()/100); // percent
			
			if(progress > currentProgress) {
				currentProgress = progress;
				progressConsumer.accept(currentProgress);
//				System.out.println("progress" + currentProgress);
//				System.out.println(visitedCount / (float) graph.getNodeCount());
//				System.out.println("visited:     " +visitedCount);
//				System.out.println("total:       " +graph.getNodeCount());
//			
//				System.out.println();
//				
				if(Thread.currentThread().isInterrupted()) {
					return;
				}
			}

			if (visited[next]) {
				continue;
			} else {
				visited[next] = true;
				visitedCount++;
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
								int distanceFromNext = distanceToVisited + graph.distanceFastInt(next, neighbour);

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
								int distanceFromNext = distanceToVisited + graph.distanceFastInt(next, neighbour);

								int beforeNeighbour = next;
								int currNeighbour = neighbour;
								// while(true)
								int nextNeighbour = graph.neighbourOf(
										currNeighbour, beforeNeighbour);
								while (graph.neighbourCount(currNeighbour) == 2
										&& !visited[nextNeighbour]) {
									visited[currNeighbour] = true;
									visitedCount++;
									
									successor[currNeighbour] = beforeNeighbour;
									distanceFromNext+= graph.distanceFastInt(beforeNeighbour, currNeighbour);
									
									beforeNeighbour = currNeighbour;
									currNeighbour = nextNeighbour;
									nextNeighbour = graph.neighbourOf(
											currNeighbour, beforeNeighbour);
									
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
		progressConsumer.accept(100);
		
		int visitedCountOfArray = 0;
		for(boolean b : visited) {
			if(b) {
				visitedCountOfArray++;
			}
		}
		System.out.println("arrayVisited:" +visitedCountOfArray);
		System.out.println("visited:     " +visitedCount);
		System.out.println("total:       " +graph.getNodeCount());
		

	}

}
