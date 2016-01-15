package osm.map;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

import org.apache.commons.lang3.SerializationUtils;

import osm.preprocessing.PipelineParts.PipelinePaths;
import osmlab.sink.GeoUtils;

public class Graph {

	/**
	 * Gives the offset in the data array for a given node id.
	 */
	private final int[] offsets;
	
	/**
	 * Contains lat/lon of each node as well as edges from each node to other nodes
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
	public static Graph createGraph(String sourceFilePath) throws FileNotFoundException {

		PipelinePaths paths = new PipelinePaths(sourceFilePath);

		int[] data = SerializationUtils.deserialize(new FileInputStream(paths.DATA_ARRAY));
		int[] offsets = SerializationUtils.deserialize(new FileInputStream(paths.OFFSET_ARRAY));
		
		return new Graph (data, offsets);
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
		int offset = offsets[node]+1;
		int lonBits = data[offset];
		float lon = Float.intBitsToFloat(lonBits);
		return lon;
	}


	/**
	 * @return Number of nodes == all ids if iterated up to, but excluding this number
	 */
	public int getNodeCount() {
		return offsets.length;
	}

	/**
	 * Calculates the distance between the coordinates of the given node and the lat and lon supplied.
	 * 
	 * @param node distance from this node
	 * @param toLat to this lat
	 * @param toLon and this lon
	 * 
	 * @return distance in meters.
	 */
	public float distance(int node, float toLat, float toLon) {
		float latOfNode = latOf(node);
		float lonOfNode = lonOf(node);
		
		float distance = GeoUtils.distFrom(latOfNode, lonOfNode, toLat, toLon);
		
		return distance;
	}
	
	
	/**
	 * Finds closest node using distance comparison with all existing nodes
	 * 
	 * @param toLat
	 * @param toLon
	 * @return
	 */
	public int findNodeClosestTo(float toLat, float toLon) {
		// TODO , naive algorithm
		float minDist = Integer.MAX_VALUE;
		int minDistNode = 0;
		
		for(int i = 0; i < getNodeCount(); i++) {

			float distance = distance(i, toLat, toLon);
			
			if(distance < minDist) {
				minDist = distance;
				minDistNode = i;
			}
		}
		
		return minDistNode;
	}
	

}
