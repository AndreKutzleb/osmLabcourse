package osm.preprocessing.pipeline;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.function.IntConsumer;

import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;

import com.carrotsearch.hppc.LongOpenHashSet;

import osm.preprocessing.DataProcessor;
import osm.preprocessing.PipelineParts.PipelinePaths;
import osm.preprocessing.pipeline.ExtractHighwayNodes.HighwaySink;
import osmlab.io.SimpleSink;
import osmlab.proto.OsmLight;
import osmlab.sink.OsmUtils;

public class FilterDuplicatesAndSortHighwayNodes extends DataProcessor {

	public FilterDuplicatesAndSortHighwayNodes(PipelinePaths paths, IntConsumer progressHandler) {
		super(paths,progressHandler);
	}

	@Override
	public void process() throws IOException {
		try (DataInputStream highwayNodesSizes = new DataInputStream(new FileInputStream(paths.HIGHWAY_NODES_RAW_DATA));
				DataInputStream highwayNodes = new DataInputStream(new BufferedInputStream(new FileInputStream(paths.HIGHWAY_NODES_RAW)));
				DataOutputStream highwayNodesSorted = new DataOutputStream( new BufferedOutputStream(new FileOutputStream(paths.HIGHWAY_NODES_SORTED)));
				DataOutputStream highwayNodesSortedSizes = new DataOutputStream(new FileOutputStream(paths.HIGHWAY_NODES_SORTED_DATA));
				) {
			int nodeCount = (int) highwayNodesSizes.readInt();
			long[] allNodes = new long[nodeCount];
			
			// read raw unsorted node ids with duplicates
			for(int i = 0; i < nodeCount; i++) {
				allNodes[i] = highwayNodes.readLong();
			}
			
			//sort according to id number
			Arrays.sort(allNodes);
			
			// write to file, but skip duplicates
			int uniqueNodes = 0;
			long previous = allNodes[0] - 1;
			for(long id : allNodes) {
				if(id != previous) {
					uniqueNodes++;
					highwayNodesSorted.writeLong(id);
				} 
				previous = id;
			}
			// write number of unique nodes to meta file
			highwayNodesSortedSizes.writeInt(uniqueNodes);
		}
		
	}


}
