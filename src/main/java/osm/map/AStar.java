package osm.map;

import it.unimi.dsi.fastutil.ints.Int2IntAVLTreeMap;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.IntArrayPriorityQueue;
import it.unimi.dsi.fastutil.ints.IntComparator;
import it.unimi.dsi.fastutil.ints.IntHeapIndirectPriorityQueue;
import it.unimi.dsi.fastutil.ints.IntHeapSemiIndirectPriorityQueue;
import it.unimi.dsi.fastutil.ints.IntPriorityQueue;

public class AStar {

	public static IntComparator getDijkstraComparator(Int2IntMap distanceToStart) {
		IntComparator dijkstraComparator = new IntComparator() {

			@Override
			public int compare(Integer k1, Integer k2) {
				throw new UnsupportedOperationException();
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

	public static void main(String[] args) {
		{

			int[] refArray = new int[21];
			refArray[5] = 1;
			refArray[20] = 2;
			refArray[3] = 3;
			refArray[10] = 0;

			Int2IntMap distanceToStart = new Int2IntAVLTreeMap();
			distanceToStart.put(10, 1);
			distanceToStart.put(5, 2);
			distanceToStart.put(3, 7);
			distanceToStart.put(20, 30);
			// IntPriorityQueue p = new IntArrayPriorityQueue();
			IntHeapIndirectPriorityQueue p = new IntHeapIndirectPriorityQueue(
					refArray);
			p.enqueue(20);
			p.enqueue(10);
			p.enqueue(5);
			p.enqueue(3);
			// p.allChanged();
			
			System.out.println("---");
			while (!p.isEmpty()) {
				System.out.println(p.dequeue());
			}
			
			p.enqueue(20);
			p.enqueue(10);
			p.enqueue(5);
			p.enqueue(3);
			
			System.out.println("----");
			while (!p.isEmpty()) {
				System.out.println(p.dequeue());
			}
			
			p.enqueue(20);
			p.enqueue(10);
			p.enqueue(5);
			p.enqueue(3);
			refArray[10] = 50;
			p.changed(10);
			System.out.println("----");
			while (!p.isEmpty()) {
				System.out.println(p.dequeue());
			}
	
	
			
			
		}
	}

	Graph graph;

	/*
	 * public void aStarPath(int from, int to) { final IntSet visited = new
	 * IntOpenHashSet(); final Int2IntMap distanceToStart = new
	 * Int2IntAVLTreeMap(); final Int2IntMap successor = new
	 * Int2IntAVLTreeMap();
	 * 
	 * distanceToStart.defaultReturnValue(-1);
	 * 
	 * IntHeapPriorityQueue queue = new IntHeapPriorityQueue(
	 * getDijkstraComparator(distanceToStart));
	 * 
	 * distanceToStart.defaultReturnValue(-1);
	 * 
	 * 
	 * queue = new IntHeapIndirectPriorityQueue(null,
	 * Dijkstra.getDijkstraComparator(distanceToStart));
	 * 
	 * queue.enqueue(x); queue.enqueue(fromNode); distanceToStart.put(fromNode,
	 * 0);
	 * 
	 * while (!queue.isEmpty()) { // if(queue.size() % 1000 == 0) { //
	 * System.out.println(queue.size()); // System.out.println("queueSize " +
	 * visited.size()); // } int next = queue.dequeueInt();
	 * if(visited.contains(next)) { continue; } visited.add(next); int
	 * distanceToVisited = distanceToStart.get(next);
	 * 
	 * if (next == toNode) { break; }
	 * 
	 * // float geoDistToClick = graph.distance(next, toLat, toLon); //
	 * if(geoDistToClick < shortestFoundDistance) { // shortestFoundDistance =
	 * geoDistToClick; // shortestFoundDistanceNode = next; // }
	 * 
	 * graph.forEachNeighbourOf( next, (neighbour) -> { if
	 * (!visited.contains(neighbour) && graph.distance(neighbour, middle.lat,
	 * middle.lon) < maxRadius) { // See if we can get there faster int
	 * distanceToStartOfNeighbour = distanceToStart .get(neighbour); int
	 * distanceFromNext = distanceToVisited + 1;
	 * 
	 * if (distanceFromNext < distanceToStartOfNeighbour ||
	 * distanceToStartOfNeighbour == -1) { // can get there faster
	 * distanceToStart .put(neighbour, distanceFromNext);
	 * successor.put(neighbour, next); } // may cause multiple instances of ways
	 * to // same node, but closest way will come first
	 * queue.enqueue(neighbour); } }); }
	 * 
	 * IntList path = new IntArrayList();
	 * 
	 * int current = toNode; path.add(toNode); while((current =
	 * successor.get(current)) != fromNode) { path.add(current); }
	 * path.add(fromNode); // TODO reverse return path; }
	 */
}
