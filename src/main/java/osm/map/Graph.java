package osm.map;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.function.IntConsumer;

import org.apache.commons.lang3.SerializationUtils;

import osm.preprocessing.PipelineParts.PipelinePaths;
import osmlab.sink.ByteUtils;
import osmlab.sink.GeoUtils;

public class Graph {

	/**
	 * Gives the offset in the data array for a given node id.
	 */
	private final int[] offsets;

	/**
	 * Contains lat/lon of each node as well as edges from each node to other
	 * nodes
	 */
	private final int[] data;

	/**
	 * Loads the graph datastructure from disc.
	 * 
	 * @param sourceFilePath
	 * @return Full graph.
	 * 
	 * @throws FileNotFoundException
	 */
	public static Graph createGraph(String sourceFilePath)
			throws FileNotFoundException {

		PipelinePaths paths = new PipelinePaths(sourceFilePath);

		int[] data = SerializationUtils.deserialize(new FileInputStream(
				paths.DATA_ARRAY));
		int[] offsets = SerializationUtils.deserialize(new FileInputStream(
				paths.OFFSET_ARRAY));

		return new Graph(data, offsets);
	}

	public Graph(int[] data, int[] offsets) {
		this.data = data;
		this.offsets = offsets;
	}

	/**
	 * @param node
	 * @return Longitude of given node
	 */
	public float latOf(int node) {
		int offset = offsets[node];
		int latBits = data[offset];
		float lat = Float.intBitsToFloat(latBits);
		return lat;
	}

	/**
	 * @param node
	 * @return Latitude of given node
	 */
	public float lonOf(int node) {
		int offset = offsets[node] + 1;
		int lonBits = data[offset];
		float lon = Float.intBitsToFloat(lonBits);
		return lon;
	}

	/**
	 * @return Number of nodes == all ids if iterated up to, but excluding this
	 *         number
	 */
	public int getNodeCount() {
		return offsets.length;
	}

	/**
	 * Calculates the distance between the coordinates of the given node and the
	 * lat and lon supplied.
	 * 
	 * @param node
	 *            distance from this node
	 * @param toLat
	 *            to this lat
	 * @param toLon
	 *            and this lon
	 * 
	 * @return distance in meters.
	 */
	public float distance(int node, float toLat, float toLon) {
		float latOfNode = latOf(node);
		float lonOfNode = lonOf(node);

		float distance = GeoUtils.distFrom(latOfNode, lonOfNode, toLat, toLon);

		return distance;
	}

	public float distanceFast(int node, float lat, float lon) {
		float latOfNode = latOf(node);
		float lonOfNode = lonOf(node);

		float y = latOfNode - lat;
		float x = lonOfNode - lon;
		float dist = (float) Math.sqrt(x * x + y * y);
		return dist;
	}

	// /**
	// * Finds closest node using distance comparison with all existing nodes
	// *
	// * @param toLat
	// * @param toLon
	// * @return
	// */
	// public int findNodeClosestTo(float toLat, float toLon) {
	// // TODO , naive algorithm
	// float minDist = Integer.MAX_VALUE;
	// int minDistNode = 0;
	//
	// for (int i = 0; i < getNodeCount(); i++) {
	//
	// float distance = distance(i, toLat, toLon);
	//
	// if (distance < minDist) {
	// minDist = distance;
	// minDistNode = i;
	// }
	// }
	//
	// return minDistNode;
	// }
	//
	// public int findNodeClosestTo(double toLat, double toLon) {
	// return findNodeClosestTo((float) toLat, (float) toLon);
	// }

	public void forEachNeighbourOf(int node, IntConsumer action) {
		int offset = offsets[node] + 2; // skip lat and lon
		int upperLimit;
		if (node + 1 == getNodeCount()) {
			upperLimit = getNodeCount();
		} else {
			upperLimit = offsets[node + 1];
		}
		for (int i = offset; i < upperLimit; i++) {
			int neigbour = ByteUtils.decodeNeighbour(data[i]);
			action.accept(neigbour);
		}
	}

	public void forEachNeighbourOf(int node, IntConsumer action, int depth) {
		if (depth > 0) {
			forEachNeighbourOf(node, neighbour -> {
				action.accept(neighbour);
				forEachNeighbourOf(neighbour, action, depth - 1);
			});

		}
	}

	public int neighbourCount(int node) {
		int offset = offsets[node] + 2; // skip lat and lon
		int upperLimit;
		if (node + 1 == getNodeCount()) {
			upperLimit = getNodeCount();
		} else {
			upperLimit = offsets[node + 1];
		}
		return upperLimit - offset;
	}

	public void forEachEdgeOf(int node, IntBiConsumer action) {
		int offset = offsets[node] + 2; // skip lat and lon
		int upperLimit;
		if (node + 1 == getNodeCount()) {
			upperLimit = getNodeCount();
		} else {
			upperLimit = offsets[node + 1];
		}
		for (int i = offset; i < upperLimit; i++) {
			int neigbour = ByteUtils.decodeNeighbour(data[i]);
			action.accept(node, neigbour);
		}
	}

	public void forEachEdgeOf(int node, IntBiConsumer action, int depth) {
		if (depth > 0) {
			forEachEdgeOf(node, (from, to) -> {
				action.accept(from, to);
				forEachEdgeOf(to, action, depth - 1);
			});

		}
	}

}
