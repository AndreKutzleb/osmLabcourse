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
import java.util.List;

import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;

import osm.preprocessing.DataProcessor;
import osm.preprocessing.PipelineParts.PipelinePaths;
import osmlab.io.AbstractHighwaySink;
import osmlab.io.SimpleSink;
import osmlab.sink.FormatConstants;
import osmlab.sink.OsmUtils;
import osmlab.sink.OsmUtils.TriConsumer;

public class CreateOffsetArray extends DataProcessor{
	

	public CreateOffsetArray(PipelinePaths paths, TriConsumer<String, Integer, Integer> progressHandler) {
		super(paths,progressHandler);
	}

	@Override
	public void process() throws IOException {
		try (DataInputStream highwayNodesSortedSizes = new DataInputStream(new FileInputStream(paths.HIGHWAY_NODES_SORTED_SIZE));
				DataInputStream highwayNodesSorted = new DataInputStream(new BufferedInputStream(new FileInputStream(paths.HIGHWAY_NODES_SORTED)));
				InputStream is = new FileInputStream(paths.SOURCE_FILE);
				DataOutputStream offsetArrayRaw = new DataOutputStream( new BufferedOutputStream(new FileOutputStream(paths.OFFSET_ARRAY_RAW)));
				DataOutputStream dataArraySize = new DataOutputStream( new BufferedOutputStream(new FileOutputStream(paths.DATA_ARRAY_SIZE)));
				DataOutputStream aggregateOffsetArrayRaw = new DataOutputStream( new BufferedOutputStream(new FileOutputStream(paths.AGGREGATE_OFFSET_ARRAY_RAW)));
				DataOutputStream aggregateDataArraySize = new DataOutputStream( new BufferedOutputStream(new FileOutputStream(paths.AGGREGATE_DATA_ARRAY_SIZE)));

				
				) {
			int nodeCount = (int) highwayNodesSortedSizes.readInt();
			long[] allNodes = new long[nodeCount];
			int[] outgoingEdgesOfNode = new int[nodeCount];
			int[] outgoingAggregateEdgesOfNode = new int[nodeCount];
			
			// read raw unsorted node ids with duplicates
			for(int i = 0; i < nodeCount; i++) {
				allNodes[i] = highwayNodesSorted.readLong();
				
				if(i % (nodeCount / 100) == 0) {
					progressHandler.accept("Reading raw Node IDs", i, nodeCount);				
				}
			}
			
			SimpleSink s = new AbstractHighwaySink() {
				
				private int highways = 0;
				private final int expectedHighways = (int) (CreateOffsetArray.this.sourceFileSize * FormatConstants.highwaysPerByte);

				@Override
				public void handleHighway(Way way, HighwayInfos info) {
					
					List<WayNode> wayNodes = way.getWayNodes();
					
					processNormal(wayNodes,info);
					processAggregate(wayNodes,info);
			
					highways++;
			
					if(highways % (expectedHighways / 100) == 0) {
						progressHandler.accept("Counting outgoing edges for each highway node", highways, expectedHighways);				
					}
				}
				
				private void processAggregate(List<WayNode> wayNodes, HighwayInfos info) {
					int indexOfFirstNode = Arrays.binarySearch(allNodes, wayNodes.get(0).getNodeId());
					int indexOfLastNode = Arrays.binarySearch(allNodes, wayNodes.get(wayNodes.size()-1).getNodeId());
		
					outgoingAggregateEdgesOfNode[indexOfFirstNode]++;					
					if(!info.Oneway) {
						outgoingAggregateEdgesOfNode[indexOfLastNode]++;							
					}					
				}

				private void processNormal(List<WayNode> wayNodes, HighwayInfos info) {
					// remember one link for each direction
					for(int i = 1; i < wayNodes.size(); i++) {
						
						long firstNode = wayNodes.get(i-1).getNodeId();
						long secondNode = wayNodes.get(i).getNodeId();
				
						int indexOfFirstNode = Arrays.binarySearch(allNodes, firstNode);
						int indexOfSecondNode = Arrays.binarySearch(allNodes, secondNode);
						
						outgoingEdgesOfNode[indexOfFirstNode]++;
						if(!info.Oneway) {
							outgoingEdgesOfNode[indexOfSecondNode]++;							
						}
					}	
				}

				@Override
				public void complete() {
					try {
						completeNormal();
						completeAggregate();
					
					} catch (IOException e) {
						e.printStackTrace();
					}
				}

				private void completeAggregate() throws IOException {
					
					int distanceFromStart = 0;
				// offset[0] is 0
				// offset[1] is offset[0] + edges[0]
				// offset[n] is offset[n-1] + edges[n-1] + constant size of entry
				int previousOffsetToStart = 0; 
				aggregateOffsetArrayRaw.writeInt(distanceFromStart); // 0 at start
				for(int i = 1; i < outgoingAggregateEdgesOfNode.length; i++) {
					previousOffsetToStart += FormatConstants.CONSTANT_NODESIZE; // space for lat/lon
					previousOffsetToStart += outgoingAggregateEdgesOfNode[i-1] * FormatConstants.CONSTANT_EDGESIZE; // space for neighbours
					aggregateOffsetArrayRaw.writeInt(previousOffsetToStart);
					
					if(i % (outgoingAggregateEdgesOfNode.length / 100) == 0) {
						progressHandler.accept("Converting aggregate edge count to offset array", i, outgoingAggregateEdgesOfNode.length);				
					}
				}
				int totalLength = previousOffsetToStart;
				totalLength+= FormatConstants.CONSTANT_NODESIZE; // space for lat/lon
				totalLength += (outgoingAggregateEdgesOfNode[outgoingAggregateEdgesOfNode.length-1] * FormatConstants.CONSTANT_EDGESIZE); // space for neighbours of last node
				aggregateDataArraySize.writeInt(totalLength);
				}

				private void completeNormal() throws IOException {
					int distanceFromStart = 0;
					// offset[0] is 0
					// offset[1] is offset[0] + edges[0]
					// offset[n] is offset[n-1] + edges[n-1] + constant size of entry
					int previousOffsetToStart = 0; 
					offsetArrayRaw.writeInt(distanceFromStart); // 0 at start
					for(int i = 1; i < outgoingEdgesOfNode.length; i++) {
						previousOffsetToStart += FormatConstants.CONSTANT_NODESIZE; // space for lat/lon
						previousOffsetToStart += outgoingEdgesOfNode[i-1] * FormatConstants.CONSTANT_EDGESIZE; // space for neighbours
						offsetArrayRaw.writeInt(previousOffsetToStart);
						
						if(i % (outgoingEdgesOfNode.length / 100) == 0) {
							progressHandler.accept("Converting edge count to offset array", i, outgoingEdgesOfNode.length);				
						}
					}
					int totalLength = previousOffsetToStart;
					totalLength+= FormatConstants.CONSTANT_NODESIZE; // space for lat/lon
					totalLength += (outgoingEdgesOfNode[outgoingEdgesOfNode.length-1] * FormatConstants.CONSTANT_EDGESIZE); // space for neighbours of last node
					dataArraySize.writeInt(totalLength);
					
					
					int maxGrade = 0;
					for(int i = 0; i < outgoingEdgesOfNode.length; i++) {
						maxGrade = Math.max(maxGrade, outgoingEdgesOfNode[i]);
					}
					System.out.println("max outgoing edges of any node: " + maxGrade);					
				}
			};
			
			OsmUtils.readFromOsm(s, is);

			
		}
		
	}
	


}
