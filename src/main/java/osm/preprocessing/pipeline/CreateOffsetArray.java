package osm.preprocessing.pipeline;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.function.IntConsumer;

import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;

import osm.preprocessing.DataProcessor;
import osm.preprocessing.PipelineParts.PipelinePaths;
import osmlab.io.AbstractHighwaySink;
import osmlab.io.SimpleSink;
import osmlab.sink.OsmUtils;

public class CreateOffsetArray extends DataProcessor{
	
	public static final int CONSTANT_NODESIZE = 2; // float lat, float lon, 4 byte each -> 8 byte or 64 bit 

	public CreateOffsetArray(PipelinePaths paths, IntConsumer progressHandler) {
		super(paths,progressHandler);
	}

	@Override
	public void process() throws IOException {
		try (DataInputStream highwayNodesSortedSizes = new DataInputStream(new FileInputStream(paths.HIGHWAY_NODES_SORTED_DATA));
				DataInputStream highwayNodesSorted = new DataInputStream(new BufferedInputStream(new FileInputStream(paths.HIGHWAY_NODES_SORTED)));
				InputStream is = new FileInputStream(paths.SOURCE_FILE);
				DataOutputStream offsetArrayRaw = new DataOutputStream( new BufferedOutputStream(new FileOutputStream(paths.OFFSET_ARRAY_RAW)));
				) {
			int nodeCount = (int) highwayNodesSortedSizes.readInt();
			long[] allNodes = new long[nodeCount];
			int[] outgoingEdgesOfNode = new int[nodeCount];
			
			// read raw unsorted node ids with duplicates
			for(int i = 0; i < nodeCount; i++) {
				allNodes[i] = highwayNodesSorted.readInt();
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
						
						outgoingEdgesOfNode[indexOfFirstNode]++;
						outgoingEdgesOfNode[indexOfSecondNode]++;
					}		
				}
				
				@Override
				public void complete() {
					try {
						int distanceFromStart = 0;
						// offset[0] is 0
						// offset[1] is offset[0] + edges[0]
						// offset[n] is offset[n-1] + edges[n-1] + constant size of entry
						int previousOffsetToStart = 0; 
						offsetArrayRaw.writeInt(distanceFromStart); // 0 at start
						for(int i = 1; i < outgoingEdgesOfNode.length; i++) {
							previousOffsetToStart += CONSTANT_NODESIZE; // space for lat/lon
							previousOffsetToStart += outgoingEdgesOfNode[i-1]; // space for neighbours
							offsetArrayRaw.writeInt(previousOffsetToStart);
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			};
			
			OsmUtils.readFromOsm(is, s);

			
		}
		
	}
	


}
