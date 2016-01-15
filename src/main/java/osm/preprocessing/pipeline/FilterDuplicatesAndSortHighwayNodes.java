package osm.preprocessing.pipeline;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.function.BiConsumer;

import osm.preprocessing.DataProcessor;
import osm.preprocessing.PipelineParts.PipelinePaths;
import osmlab.sink.OsmUtils.TriConsumer;

public class FilterDuplicatesAndSortHighwayNodes extends DataProcessor {

	public FilterDuplicatesAndSortHighwayNodes(PipelinePaths paths, TriConsumer<String, Integer, Integer> progressHandler) {
		super(paths,progressHandler);
	}

	@Override
	public void process() throws IOException {
		try (DataInputStream highwayNodesSizes = new DataInputStream(new FileInputStream(paths.HIGHWAY_NODES_RAW_SIZE));
				DataInputStream highwayNodes = new DataInputStream(new BufferedInputStream(new FileInputStream(paths.HIGHWAY_NODES_RAW)));
				DataOutputStream highwayNodesSorted = new DataOutputStream( new BufferedOutputStream(new FileOutputStream(paths.HIGHWAY_NODES_SORTED)));
				DataOutputStream highwayNodesSortedSizes = new DataOutputStream(new FileOutputStream(paths.HIGHWAY_NODES_SORTED_SIZE));
				) {
			int nodeCount = (int) highwayNodesSizes.readInt();
			long[] allNodes = new long[nodeCount];
			
			// read raw unsorted node ids with duplicates
			for(int i = 0; i < nodeCount; i++) {
				allNodes[i] = highwayNodes.readLong();
				
				if(i % (nodeCount / 100) == 0) {
					progressHandler.accept("Reading raw Node IDs", i, nodeCount);				
				}
			}
			
			//sort according to id number
			Arrays.sort(allNodes);
			
			
			int progress = 0;
			// write to file, but skip duplicates
			int uniqueNodes = 0;
			long previous = allNodes[0] - 1;
			for(long id : allNodes) {
				if(id != previous) {
					uniqueNodes++;
					highwayNodesSorted.writeLong(id);
				} 
				previous = id;
				
				progress++;
				
				if(progress % (allNodes.length / 100) == 0) {
					progressHandler.accept("Writing sorted Node IDs, skipping duplicates", progress, allNodes.length);				
				}
			}
			// write number of unique nodes to meta file
			highwayNodesSortedSizes.writeInt(uniqueNodes);
			
			highwayNodesSortedSizes.flush();
			highwayNodesSorted.flush();
		}
		
	}


}
