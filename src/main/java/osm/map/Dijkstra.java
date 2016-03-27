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
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import javax.swing.JProgressBar;

import osmlab.io.AbstractHighwaySink;
import osmlab.sink.ByteUtils;
import osmlab.sink.FormatConstants;

public class Dijkstra {

	public enum TravelType {
		PEDESTRIAN("Pedestrian", Color.BLACK,false), 
		CAR_SHORTEST("Car Shortest", Color.RED,false), 
		CAR_FASTEST("Car Fastest", Color.BLUE,false), 
		HOP_DISTANCE("Hop Distance", Color.MAGENTA,false),
		PEDESTRIAN_POPULATION("Pedestrian Population", Color.RED, false),
		CAR_SHORTEST_POPULATION("Car Shortest Population",Color.WHITE, false),
		CAR_FASTEST_POPULATION("Car Fastest Population", Color.WHITE, false);
//		POPULATION_GOOD("Population good", Color.GREEN, false),
//		POPULATION_BAD("Population bad", Color.WHITE, false),
//		PEDESTRIAN_FF("Pedestrian FF", Color.BLACK,true), 
//		CAR_SHORTEST_FF("Car Shortest FF", Color.RED,true), 
//		CAR_FASTEST_FF("Car Fastest FF", Color.BLUE,true), 
//		HOP_DISTANCE_FF("Hop Distance FF", Color.MAGENTA,true), 
		
		
		private TravelType(String name, Color color, boolean fastFollow) {
			this.name = name;
			this.color = color;
			this.fastFollow = fastFollow;
		}

		public final String name;
		public final Color color;
		public final boolean fastFollow;		
		
	}


	private final Graph graph;
	public final int[] refArray;
	public final int[] successor;
	public final boolean[] visited;
	public final TravelType travelType;
	private int visitedCount = 0;
	private final Semaphore workedOn = new Semaphore(1);
	private final IntHeapIndirectPriorityQueue queue;

	private void resetData() {
		Arrays.fill(refArray, 0);
		Arrays.fill(visited, false);
		visitedCount = 0;
		queue.clear();
	}

	public Graph getGraph() {
		return graph;
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
			return Route.noPath(this,travelType);
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

		return new Route(this,path, edgeSpeeds, distances, travelType);
	}

	private void calculateAdditionalInfo(IntList path, ByteList edgeSpeeds,
			FloatList distances) {

		for (int i = 1; i < path.size(); i++) {
			int from = path.getInt(i - 1);
			int to = path.getInt(i);

			int edgeMetaData = graph.edgeMetaData(to, from);
			byte decodeSpeed = ByteUtils.decodeSpeed(edgeMetaData);
			edgeSpeeds.add(decodeSpeed);

			float distance = determineDistance(to, from,0,false);
			distances.add(distance);
		}
	}

	public void precalculateDijkstra(int fromNode, IntConsumer progressConsumer, int populationMultiplier, boolean preferPopulation, AtomicInteger currentDestination,Consumer<Route> routeHandler, Semaphore dijkstraMutex) {

		try {
			workedOn.acquire();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		int currentDest = -1;
		
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
				int newDest = currentDestination.get();
				if(newDest != currentDest) {
					if(visited[newDest]) {
						currentDest = newDest;
						Route path = getPath(currentDest);
						routeHandler.accept(path);
					}
				}

				if (Thread.currentThread().isInterrupted()) {
					System.out.println("thread is interrupted");
					workedOn.release();
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

			int offset = graph.offsets[next] + FormatConstants.CONSTANT_NODESIZE; // skip lat and lon
			int upperLimit;
			if (next + 1 == graph.nodeCount) {
				upperLimit = graph.nodeCount;
			} else {
				upperLimit = graph.offsets[next + 1];
			}
			for (int i = offset; i < upperLimit; i += FormatConstants.CONSTANT_EDGESIZE) {
				int neighbour = ByteUtils.decodeNeighbour(graph.data[i]);

				if (visited[neighbour]) {
					continue;
				}

				int distanceToStartOfNeighbour = refArray[neighbour];
				int distanceFromNext = distanceToVisited
						+ determineDistance(next, neighbour, populationMultiplier, preferPopulation);

				boolean neighbourInQueue = queue.contains(neighbour);

				// already in queue but not yet visited
				if (neighbourInQueue) {

					boolean improvement = distanceFromNext < distanceToStartOfNeighbour
							|| distanceToStartOfNeighbour == 0;

					if (improvement) {
						// can get there faster
						refArray[neighbour] = distanceFromNext;
						successor[neighbour] = next;
						queue.changed(neighbour);
					}
				} else if (this.travelType.fastFollow && graph.neighbourCount(neighbour) == 2) {
					int alreadyVisited = next;
					int toSkip = neighbour;
					while(!visited[toSkip] && graph.neighbourCount(toSkip) == 2) {
						visitedCount++;
						visited[toSkip] = true;
						refArray[toSkip] = refArray[alreadyVisited] + determineDistance(alreadyVisited, toSkip,populationMultiplier, preferPopulation);
						successor[toSkip] = alreadyVisited;
						
						int nextToVisit = graph.neighbourOf(toSkip, alreadyVisited);
						alreadyVisited = toSkip;
						toSkip = nextToVisit;
					}
					if(!visited[toSkip] && !queue.contains(toSkip)) {
						refArray[toSkip] = refArray[alreadyVisited] + determineDistance(alreadyVisited, toSkip,populationMultiplier, preferPopulation);
						successor[toSkip] = alreadyVisited;
						queue.enqueue(toSkip);
					} else if (queue.contains(toSkip)){
						// need to update if better
						int distanceToSkip = refArray[alreadyVisited] + determineDistance(alreadyVisited, toSkip,populationMultiplier, preferPopulation);
						boolean improvement = distanceToSkip < refArray[toSkip];

						if (improvement) {
							// can get there faster
							refArray[toSkip] = distanceToSkip;
							successor[toSkip] = alreadyVisited;
							queue.changed(toSkip);
						}
		
					}
				} else {	
					// add node for the first time
					refArray[neighbour] = distanceFromNext;
					successor[neighbour] = next;
					queue.enqueue(neighbour);

				}

			}

		}
		
		int newDest = currentDestination.get();
		if(newDest != currentDest) {
			if(visited[newDest]) {
				currentDest = newDest;
				Route path = getPath(currentDest);
				routeHandler.accept(path);
			}
		}
		
		progressConsumer.accept(100);
		long afterDijkstra = System.currentTimeMillis();

		dijkstraDuration = afterDijkstra - afterResetData;
		
		workedOn.release();

	}

	int A_LARGE_NUMBER = 100000;

	/**
	 * 

pop bad:
pop 1   * 0 =  0  * 100 = 100 + 0   
pop 1   * 1 =  1  * 100 = 100 + 100 
pop 1   * 5 =  5  * 100 = 100 + 500
pop 1   * 10 = 10 * 100 = 100 + 1000

pop .5   * 0 =  0    * 100 = 100 + 0   
pop .5   * 1 =  .5   * 100 = 100 + 50 
pop .5   * 5 =  2.5  * 100 = 100 + 250
pop .5   * 10 = 5    * 100 = 100 + 500

pop good:

pop 1   * 0 =  0  * 100 = 100 + (0-0)   
pop 1   * 1 =  1  * 100 = 100 + (100-100)  
pop 1   * 5 =  5  * 100 = 100 + (500-500)
pop 1   * 10 = 10 * 100 = 100 + (1000-1000)

pop .5   * 0 =  0    * 100 = 100 + (0-0)   
pop .5   * 1 =  .5   * 100 = 100 + (100-50) 
pop .5   * 5 =  2.5  * 100 = 100 + (500-250)
pop .5   * 10 = 5    * 100 = 100 + (1000-500)


	 * 
	 * @param from
	 * @param to
	 * @param populationMultiplier
	 * @return
	 */
	private int determineDistance(int from, int to, int populationMultiplier, boolean preferPopulation) {

		//
		int edgeMetaData = graph.edgeMetaData(from, to);
		byte speed = ByteUtils.decodeSpeed(edgeMetaData);
		boolean pedestrian = ByteUtils.decodePedestrian(edgeMetaData);

		float distanceFloat = graph.distance(from, to);

	//	int distance = (int) Math.max(1, distanceFloat);

		switch (travelType) {
			case PEDESTRIAN : 
			//case PEDESTRIAN_FF: 
			{
				if (pedestrian) {
					return (int) Math.max(1, distanceFloat);
				} else {
					return A_LARGE_NUMBER;
				}
			}

			case CAR_SHORTEST :
			//case CAR_SHORTEST_FF : 
			{
				if (speed == 0) {
					return A_LARGE_NUMBER;
				} else {

					return (int) Math.max(1, distanceFloat);
				}
			}
			case CAR_FASTEST :
			// case CAR_FASTEST_FF : 
			{
				if (speed == 0) {
					return (int) (Math.max(1, distanceFloat) + A_LARGE_NUMBER);
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
			//case HOP_DISTANCE_FF :
				return 1;
			

			case PEDESTRIAN_POPULATION:
			{
				if (pedestrian) {
					
					float population = graph.population[to];
					float popCost = populationMultiplier * population
							* distanceFloat;
					
					float actualCost;
					if (preferPopulation) {
						float worstCaseCost = populationMultiplier
								* distanceFloat;
						actualCost = distanceFloat + (worstCaseCost - popCost);
					} else {
						actualCost = distanceFloat + popCost;
					}
					return (int) Math.max(1, actualCost);

				} else {
					return A_LARGE_NUMBER;
				}
			}
			
			case CAR_SHORTEST_POPULATION :
				//case CAR_SHORTEST_FF : 
				{
					if (speed == 0) {
						return A_LARGE_NUMBER;
					} else {
						
						
						float population = graph.population[to];
						float popCost = populationMultiplier * population
								* distanceFloat;
						
						float actualCost;
						if (preferPopulation) {
							float worstCaseCost = populationMultiplier
									* distanceFloat;
							actualCost = distanceFloat + (worstCaseCost - popCost);
						} else {
							actualCost = distanceFloat + popCost;
						}
						return (int) Math.max(1, actualCost);
					}
				}
			case CAR_FASTEST_POPULATION : 
			{
				
				if (speed == 0) {
					return (int) (Math.max(1, distanceFloat) + A_LARGE_NUMBER);
				} else {

					// for car fastest, we do not use millimeters as metric, but
					// milliseconds to cross the distance with the given max
					// speed

					float kmh = AbstractHighwaySink.speedBitsToKmh(speed);

					float secondsPerMeter = 3.6f / kmh;
					
					
					float population = graph.population[to];
					float popCost = populationMultiplier * population
							* distanceFloat;
					
					float actualCost;
					if (preferPopulation) {
						float worstCaseCost = populationMultiplier
								* distanceFloat;
						actualCost = distanceFloat + (worstCaseCost - popCost);
					} else {
						actualCost = distanceFloat + popCost;
					}
					
					int timeTakenInSeconds = Math.round(actualCost
							* secondsPerMeter);

					timeTakenInSeconds = Math.max(1, timeTakenInSeconds);

					return timeTakenInSeconds;
				}
			}
				

			default :
				throw new IllegalStateException();
		}
	}

	public String getName() {
		return travelType.name;
	}

	public void manuallyTryRoute(int destinationNode, Consumer<Route> routeHandler) {
		if(workedOn.tryAcquire()) {
			workedOn.release();
			routeHandler.accept(getPath(destinationNode));
		}
		
	}

	//
	//
	//
	//
	// } else {
	//
	// }

}
