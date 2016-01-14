package osm.preprocessing.pipeline;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.function.IntConsumer;

import org.apache.commons.lang3.SerializationUtils;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;

import osm.preprocessing.DataProcessor;
import osm.preprocessing.PipelineParts.PipelinePaths;
import osmlab.io.AbstractHighwaySink;
import osmlab.io.SimpleSink;
import osmlab.sink.ByteUtils;
import osmlab.sink.OsmUtils;

public class CreateDataArray extends DataProcessor{

	public CreateDataArray(PipelinePaths paths, IntConsumer progressHandler) {
		super(paths, progressHandler);
	}

	@Override
	public void process() throws IOException {
		try (DataInputStream highwayNodesSortedSizes = new DataInputStream(new FileInputStream(paths.HIGHWAY_NODES_SORTED_SIZE));
				DataInputStream highwayNodesSorted = new DataInputStream(new BufferedInputStream(new FileInputStream(paths.HIGHWAY_NODES_SORTED)));
				DataInputStream offsetArrayRaw = new DataInputStream(new BufferedInputStream(new FileInputStream(paths.OFFSET_ARRAY_RAW)));
				DataInputStream dataArraySize = new DataInputStream(new BufferedInputStream(new FileInputStream(paths.DATA_ARRAY_SIZE)));
				InputStream is = new FileInputStream(paths.SOURCE_FILE);
				ObjectOutputStream dataArrayStream = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(paths.DATA_ARRAY)));
				ObjectOutputStream offsetArrayStream = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(paths.OFFSET_ARRAY)));

				) {
			
			int nodeCount = (int) highwayNodesSortedSizes.readInt();
			int dataSize = dataArraySize.readInt();
			
			long[] allNodes = new long[nodeCount];
			int[] offsetArray = new int[nodeCount];
			int[] data = new int[dataSize];
			
			// read raw unsorted node ids with duplicates
			for(int i = 0; i < nodeCount; i++) {
				allNodes[i] = highwayNodesSorted.readInt();
				offsetArray[i] = offsetArrayRaw.readInt();
			}
			
			SimpleSink s = new AbstractHighwaySink() {
				
				@Override
				public void handleHighway(Way way) {
					// remember one link for each direction
					for(int i = 1; i < way.getWayNodes().size(); i++) {
						
						long firstNode = way.getWayNodes().get(i-1).getNodeId();
						long secondNode = way.getWayNodes().get(i).getNodeId();
				
						int indexOfFirstNode = Arrays.binarySearch(allNodes, firstNode);
						int indexOfSecondNode = Arrays.binarySearch(allNodes, secondNode);
						
						addEdgeFromTo(indexOfFirstNode,indexOfSecondNode);
						addEdgeFromTo(indexOfSecondNode,indexOfFirstNode);
						
					}		
				}
				
				private void addEdgeFromTo(int indexOfFirstNode,
						int indexOfSecondNode) {
					int offset = offsetArray[indexOfFirstNode];
					offset+= FormatConstants.CONSTANT_NODESIZE; // Skip LAT / LON. We want to enter the edge only
					// There may be neighbours already, skip to first 0-spot (assuming no neighbour yet defaults to 0 in dataArray)
					while(data[offset] != 0) {
						offset++;
					}
					// fill in data of connection - target id, speed and pedestrian yes/no
					int edge = ByteUtils.encodeEdge(indexOfSecondNode, true, (byte) 15);
					data[offset] = edge;
				}

				@Override
				public void handleNode(Node node) {
					// if the node is part of a highway, we will find it with a binary search. in that case, we add the coordinates of it to
					// data array. otherwise, its no node that is part of a highway and we skip it
					int indexOfNode = Arrays.binarySearch(allNodes, node.getId());
					// if true, this is a node which is part of a highway
					if(indexOfNode > 0) {
						int offset = offsetArray[indexOfNode];
						// First int is lat, second int is lon
						data[offset] = Float.floatToRawIntBits((float) node.getLatitude());
						data[offset+1] = Float.floatToRawIntBits((float) node.getLongitude());
					}

				}
				
				@Override
				public void complete() {
					// Write our final datastructures to disk as serialized java arrays - offsetarray and data array.
					try {
						dataArrayStream.writeObject(data);
						offsetArrayStream.writeObject(offsetArray);
					} catch (IOException e) {
						e.printStackTrace();
					}
				
				}
			};
			
			OsmUtils.readFromOsm(is, s);
		}
	}

}