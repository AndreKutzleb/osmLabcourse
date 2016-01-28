package osm.map;

import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.bytes.ByteList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntHeapIndirectPriorityQueue;
import it.unimi.dsi.fastutil.ints.IntList;

import java.util.Arrays;
import java.util.function.IntConsumer;

import osmlab.io.AbstractHighwaySink;
import osmlab.sink.ByteUtils;

public class Dijkstra {

	public enum TravelType {
		PEDESTRIAN, CAR_SHORTEST, CAR_FASTEST
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
		queue = new IntHeapIndirectPriorityQueue(refArray, graph.getNodeCount());
	}

	public Route getPath(int toNode) {

		IntList path = new IntArrayList();
		// no way possible.
		if (!visited[toNode]) {
			return Route.noPath();
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
		
		ByteList edgeSpeeds = calculateSpeeds(path);

		return Route.pathOf(path,edgeSpeeds);
	}
	
	private ByteList calculateSpeeds(IntList path) {
		ByteList speedInfo = new ByteArrayList(path.size()-1);
		for(int i = 1; i < path.size(); i++) {
			int from = path.getInt(i-1);
			int to = path.getInt(i);
			
			int edgeMetaData = graph.edgeMetaData(to, from);
			byte decodeSpeed = ByteUtils.decodeSpeed(edgeMetaData);
			speedInfo.add(decodeSpeed);
		}
		return speedInfo;
	}

	public void precalculateDijkstra(int fromNode, IntConsumer progressConsumer) {

		resetData();
		this.fromNode = fromNode;

		queue.enqueue(fromNode);

		int currentProgress = 0;

		progressConsumer.accept(0);
		while (!queue.isEmpty()) {

			int next = queue.dequeue();

			int progress = (visitedCount) / (graph.getNodeCount() / 100); // percent

			if (progress > currentProgress) {
				currentProgress = progress;
				progressConsumer.accept(currentProgress);

				if (Thread.currentThread().isInterrupted()) {
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

			graph.forEachNeighbourOf(
					next,
					(neighbour) -> {

						if (!visited[neighbour]) {

							if (queue.contains(neighbour)) {

								int distanceToStartOfNeighbour = refArray[neighbour];
								int distanceFromNext = determineDistance(
										distanceToVisited, next, neighbour);

								boolean improvement = distanceFromNext < distanceToStartOfNeighbour
										|| distanceToStartOfNeighbour == 0;

								if (improvement) {
									// can get there faster
									refArray[neighbour] = distanceFromNext;
									successor[neighbour] = next;
									queue.changed(neighbour);
								}
							}

							else {
								// fastforward
								int distanceFromNext = determineDistance(
										distanceToVisited, next, neighbour);

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
									distanceFromNext = determineDistance(
											distanceFromNext, beforeNeighbour,
											currNeighbour);

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
		for (boolean b : visited) {
			if (b) {
				visitedCountOfArray++;
			}
		}
		System.out.println("arrayVisited:" + visitedCountOfArray);
		System.out.println("visited:     " + visitedCount);
		System.out.println("total:       " + graph.getNodeCount());

	}

	int A_LARGE_NUMBER = 100000;

	private int determineDistance(int distanceToVisited, int from, int to) {

//		if(true) {
//			return distanceToVisited + 1;
//		}
//		
		int edgeMetaData = graph.edgeMetaData(from, to);
		byte speed = ByteUtils.decodeSpeed(edgeMetaData);
		boolean pedestrian = ByteUtils.decodePedestrian(edgeMetaData);
		
		float distanceFloat = graph.distance(from, to);

		int distance = (int) Math.max(1,distanceFloat);

		
		switch (travelType) {
			case PEDESTRIAN : {
				if (pedestrian) {
					return distance + distanceToVisited;
				} else {
					return distance + A_LARGE_NUMBER;
				}
			}

			case CAR_SHORTEST : {
				if (speed == 0) {
					return distance + A_LARGE_NUMBER;
				} else {

					return distance + distanceToVisited;
				}
			}
			case CAR_FASTEST : {
				if (speed == 0) {
					return distance + A_LARGE_NUMBER;
				} else {
					
					// for car fastest, we do not use millimeters as metric, but
					// milliseconds to cross the distance with the given max speed



					float kmh = AbstractHighwaySink.speedBitsToKmh(speed);
			
					float secondsPerMeter = 3.6f / kmh;
					float distanceMeters = distanceFloat / 1000f;
					
				
					int timeTakenInSeconds = Math.round(distanceMeters / secondsPerMeter);

					
					timeTakenInSeconds = Math.max(1,timeTakenInSeconds);
//					System.out.println(distance);
//					System.out.println(distanceMeters + "/" + secondsPerMeter + " = " + timeTakenInSeconds);
//					try {
//						Thread.sleep(1);
//					} catch (InterruptedException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
					return distanceToVisited + timeTakenInSeconds;
				}
			}
		}

		return 0; // TODO
	}

}
