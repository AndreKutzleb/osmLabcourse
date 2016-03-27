package osm.map;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.function.IntConsumer;

import org.apache.commons.lang3.SerializationUtils;

import osm.preprocessing.PipelineParts.PipelinePaths;
import osmlab.sink.ByteUtils;
import osmlab.sink.FormatConstants;
import osmlab.sink.GeoUtils.FloatPoint;

public class Graph {

	/**
	 * Gives the offset in the data array for a given node id.
	 */
	final int[] offsets;

	/**
	 * Contains lat/lon of each node as well as edges from each node to other
	 * nodes
	 */
	final int[] data;
	
	final float[] population;
	
	final int nodeCount;
	
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
		
		float[] population = SerializationUtils.deserialize(new FileInputStream(paths.POPULATION_ARRAY));

		return new Graph(data, offsets,population);
		
	}

	public Graph(int[] data, int[] offsets, float[] population) {
		this.data = data;
		this.offsets = offsets;
		this.population = population;
		this.nodeCount = offsets.length;
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
	public final int getNodeCount() {
		return nodeCount;
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
	 * @return distance in millimeters.
	 */
	public float distanceToCoordinates(int node, float toLat, float toLon) {
		float latOfNode = latOf(node);
		float lonOfNode = lonOf(node);

		float y = latOfNode - toLat;
		float x = lonOfNode - toLon;
		float dist = (float) Math.sqrt(x * x + y * y);
		dist*= 80_000; // approximate distannce of one longitude
		return dist;

	}
	
	public float distance(int nodeA, int nodeB) {
		int offset = offsets[nodeA] + FormatConstants.CONSTANT_NODESIZE; // skip lat and lon
		int upperLimit = getUpperLimit(nodeA);
		for (int i = offset; i < upperLimit; i+=FormatConstants.CONSTANT_EDGESIZE) {
			int neighbour = ByteUtils.decodeNeighbour(data[i]);
			if(neighbour == nodeB) {
				return Float.intBitsToFloat(data[i+1]);
			}
		}

		throw new IllegalArgumentException("no edge between " + nodeA + " and " + nodeB);
	}
	

	
	public int edgeMetaData(int from, int to) {
		int offset = offsets[from] + 2; // skip lat and lon
		int upperLimit = getUpperLimit(from);
		for (int i = offset; i < upperLimit; i++) {
			int neigbour = ByteUtils.decodeNeighbour(data[i]);
			if(neigbour == to) {
				return data[i];
			}
		}
		
		throw new IllegalStateException("to node no neighbour of from node");
	}

	
	public FloatPoint pointAtPercent(int fromNode, int toNode, float percent) {
		percent/= 100f;
		float fromLat = latOf(fromNode);
		float fromLon = lonOf(fromNode);
		
		float toLat = latOf(toNode);
		float toLon = lonOf(toNode);
		
		// make from the 0-point
		float toLatNull = toLat - fromLat;
		float toLonNull = toLon - fromLon;
		
		// shorten vector
		toLatNull*= percent;
		toLonNull*= percent;
		
		// add original origin back
		toLatNull+= fromLat;
		toLonNull+= fromLon;
		
		return new FloatPoint(toLatNull, toLonNull);
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
		int upperLimit = getUpperLimit(node);
		
		for (int i = offset; i < upperLimit; i+= FormatConstants.CONSTANT_EDGESIZE) {
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
		return neighbourCountInternal(node, offsets);
		}
	
	
	private int neighbourCountInternal(int node, int[] offsets) {
		int offset = offsets[node] + 2; // skip lat and lon
		int upperLimit = getUpperLimit(node);
		return (upperLimit - offset) / FormatConstants.CONSTANT_EDGESIZE;
	}


	public void forEachEdgeOf(int node, IntBiConsumer action) {
		int offset = offsets[node] + 2; // skip lat and lon
		int upperLimit = getUpperLimit(node);
		
		for (int i = offset; i < upperLimit; i+=FormatConstants.CONSTANT_EDGESIZE) {
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

	public int neighbourOf(int node, int notThisNeighbour) {
		int offset = offsets[node] + 2; // skip lat and lon
		int upperLimit = getUpperLimit(node);
		
		for (int i = offset; i < upperLimit; i+=FormatConstants.CONSTANT_EDGESIZE) {
			int neighbour = ByteUtils.decodeNeighbour(data[i]);
			if(neighbour != notThisNeighbour) {
				return neighbour;
			}
		}
		return ByteUtils.decodeNeighbour(data[offset]);
	}

	public void forEachEdge(int node, IntConsumer action) {
		int offset = offsets[node] + FormatConstants.CONSTANT_NODESIZE; // skip lat and lon
		int upperLimit = getUpperLimit(node);
		
		for (int i = offset; i < upperLimit; i+=FormatConstants.CONSTANT_EDGESIZE) {
			int neighbour = ByteUtils.decodeNeighbour(data[i]);
			action.accept(neighbour);
		}		
	}
	
	private int getUpperLimit(int node) {
		if (node + 1 == getNodeCount()) {
			return getNodeCount();
		} else {
			return offsets[node + 1];
		}
	}
	
	


}
