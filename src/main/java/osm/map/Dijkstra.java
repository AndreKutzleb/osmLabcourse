package osm.map;

import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.bytes.ByteList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntHeapIndirectPriorityQueue;
import it.unimi.dsi.fastutil.ints.IntList;

import java.awt.Color;
import java.util.Arrays;
import java.util.function.IntConsumer;

import javax.swing.JProgressBar;

import osmlab.io.AbstractHighwaySink;
import osmlab.sink.ByteUtils;

public class Dijkstra {

	public enum TravelType {
		PEDESTRIAN("Pedestrian", Color.BLACK), CAR_SHORTEST("Car Shortest",
				Color.RED), CAR_FASTEST("Car Fastest", Color.BLUE), HOP_DISTANCE(
				"Hop Distance", Color.MAGENTA);

		private TravelType(String name, Color color) {
			this.name = name;
			this.color = color;
		}

		private final String name;
		private final Color color;

		public String getName() {
			return name;
		}

		public Color getColor() {
			return color;
		}
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
	public volatile long resetDuration;
	public volatile long dijkstraDuration;
	public final JProgressBar progress;

	public Dijkstra(Graph graph, TravelType travelType, JProgressBar progress) {
		this.travelType = travelType;
		this.graph = graph;
		this.progress = progress;
		refArray = new int[graph.getNodeCount()];
		successor = new int[graph.getNodeCount()];
		visited = new boolean[graph.getNodeCount()];
		queue = new IntHeapIndirectPriorityQueue(refArray, graph.getNodeCount());
	}

	public Route getPath(int toNode) {

		IntList path = new IntArrayList();
		// no way possible.
		if (!visited[toNode]) {
			return Route.noPath(travelType);
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

		ByteList edgeSpeeds = new ByteArrayList(path.size() - 1);
		FloatList distances = new FloatArrayList(path.size() - 1);

		calculateAdditionalInfo(path, edgeSpeeds, distances);

		return new Route(path, edgeSpeeds, distances,travelType);
	}

	private void calculateAdditionalInfo(IntList path, ByteList edgeSpeeds,
			FloatList distances) {

		for (int i = 1; i < path.size(); i++) {
			int from = path.getInt(i - 1);
			int to = path.getInt(i);

			int edgeMetaData = graph.edgeMetaData(to, from);
			byte decodeSpeed = ByteUtils.decodeSpeed(edgeMetaData);
			edgeSpeeds.add(decodeSpeed);

			float distance = graph.distance(to, from);
			distances.add(distance);
		}
	}

	public void precalculateDijkstra(int fromNode, IntConsumer progressConsumer) {

		long beforeResetData = System.currentTimeMillis();
		resetData();
		long afterResetData = System.currentTimeMillis();

		resetDuration = afterResetData - beforeResetData;

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
							int distanceToStartOfNeighbour = refArray[neighbour];
							int distanceFromNext = distanceToVisited
									+ determineDistance(next, neighbour);

							// already in queue but not yet visited
							if (queue.contains(neighbour)) {

								boolean improvement = distanceFromNext < distanceToStartOfNeighbour
										|| distanceToStartOfNeighbour == 0;

								if (improvement) {
									// can get there faster
									refArray[neighbour] = distanceFromNext;
									successor[neighbour] = next;
									queue.changed(neighbour);
								}
							} else {
								// add node for the first time
								refArray[neighbour] = distanceFromNext;
								successor[neighbour] = next;
								queue.enqueue(neighbour);
							}
						}

					});
		}
		progressConsumer.accept(100);

		long afterDijkstra = System.currentTimeMillis();

		dijkstraDuration = afterDijkstra - afterResetData;

	}

	int A_LARGE_NUMBER = 100000;

	private int determineDistance(int from, int to) {

		//
		int edgeMetaData = graph.edgeMetaData(from, to);
		byte speed = ByteUtils.decodeSpeed(edgeMetaData);
		boolean pedestrian = ByteUtils.decodePedestrian(edgeMetaData);

		float distanceFloat = graph.distance(from, to);

		int distance = (int) Math.max(1, distanceFloat);

		switch (travelType) {
			case PEDESTRIAN : {
				if (pedestrian) {
					return distance;
				} else {
					return A_LARGE_NUMBER;
				}
			}

			case CAR_SHORTEST : {
				if (speed == 0) {
					return A_LARGE_NUMBER;
				} else {

					return distance;
				}
			}
			case CAR_FASTEST : {
				if (speed == 0) {
					return distance + A_LARGE_NUMBER;
				} else {

					// for car fastest, we do not use millimeters as metric, but
					// milliseconds to cross the distance with the given max
					// speed

					float kmh = AbstractHighwaySink.speedBitsToKmh(speed);

					float secondsPerMeter = 3.6f / kmh;

					int timeTakenInSeconds = Math.round(distanceFloat
							* secondsPerMeter);

					timeTakenInSeconds = Math.max(1, timeTakenInSeconds);
					// System.out.println(distance);
					// System.out.println(distanceMeters + "/" + secondsPerMeter
					// + " = " + timeTakenInSeconds);
					// try {
					// Thread.sleep(1);
					// } catch (InterruptedException e) {
					// // TODO Auto-generated catch block
					// e.printStackTrace();
					// }
					return timeTakenInSeconds;
				}
			}
			case HOP_DISTANCE :
				return 1;

			default :
				throw new IllegalStateException();
		}
	}

	public String getName() {
		return travelType.getName();
	}

	// // fastforward
	// int distanceFromNext = determineDistance(
	// distanceToVisited, next, neighbour);
	//
	// int beforeNeighbour = next;
	// int currNeighbour = neighbour;
	// // while(true)
	// int nextNeighbour = graph.neighbourOf(
	// currNeighbour, beforeNeighbour);
	// while (graph.neighbourCount(currNeighbour) == 2
	// && !visited[nextNeighbour]) {
	// visited[currNeighbour] = true;
	// visitedCount++;
	//
	// successor[currNeighbour] = beforeNeighbour;
	// distanceFromNext+= determineDistance(beforeNeighbour,
	// currNeighbour);
	//
	// beforeNeighbour = currNeighbour;
	// currNeighbour = nextNeighbour;
	// nextNeighbour = graph.neighbourOf(
	// currNeighbour, beforeNeighbour);
	//
	// }

	// else {
	//
	//
	// if (queue.contains(currNeighbour)) {
	//
	// int distanceToStartOfNeighbour = refArray[currNeighbour];
	//
	// boolean improvement = distanceFromNext < distanceToStartOfNeighbour
	// || distanceToStartOfNeighbour == 0;
	//
	// if (improvement) {
	// // can get there faster
	// refArray[currNeighbour] = distanceFromNext;
	// successor[currNeighbour] = beforeNeighbour;
	// queue.changed(currNeighbour);
	// }

}
