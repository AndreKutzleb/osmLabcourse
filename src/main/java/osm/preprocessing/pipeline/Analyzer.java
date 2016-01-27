package osm.preprocessing.pipeline;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.longs.LongSets;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.BiConsumer;

import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;

import osm.preprocessing.DataProcessor;
import osm.preprocessing.PipelineParts.PipelinePaths;
import osmlab.io.AbstractHighwaySink;
import osmlab.io.SimpleSink;
import osmlab.sink.FormatConstants;
import osmlab.sink.OsmUtils;
import osmlab.sink.OsmUtils.TriConsumer;

public class Analyzer extends DataProcessor{

	public Analyzer(PipelinePaths paths, TriConsumer<String, Integer, Integer> progressHandler) {
		super(paths, progressHandler);
	}

	@Override
	public void process() throws IOException {
		try (InputStream is = new FileInputStream(paths.SOURCE_FILE)) {
			
			SimpleSink s = new AbstractHighwaySink() {
				
				private long ways = 0;
				private long highways = 0;
				private long nodes = 0;
				private LongSet highwayNodes = new LongOpenHashSet();
				
				private int expectedWays = (int) (FormatConstants.waysPerByte * Analyzer.this.sourceFileSize);
				private int expectedNodes = (int) (FormatConstants.nodesPerByte * Analyzer.this.sourceFileSize);
				private int expectedWaysAndNodes = expectedNodes+expectedWays;
				
				@Override
				public void handleWay(Way way) {
					ways++;
					
					if((ways+nodes) % (expectedWaysAndNodes/100) == 0) {
						progressHandler.accept("Analyzing size and contents of OSM file", (int) (ways+nodes), expectedWaysAndNodes);
					}
				}
				
				@Override
				public void handleHighway(Way way,HighwayInfos infos) {
					highways++;
					for(WayNode w : way.getWayNodes()) {
						highwayNodes.add(w.getNodeId());
					}
				}
			
				@Override
				public void handleNode(Node node) {
					nodes++;
					
					if((ways+nodes) % (expectedWaysAndNodes/100) == 0) {
						progressHandler.accept("Analyzing size and contents of OSM file", (int) (ways+nodes), expectedWaysAndNodes);
					}
				}
				
				@Override
				public void complete() {
					long fileSize = new File(paths.SOURCE_FILE).length();

					System.out.println("ways:         " + ways);
					System.out.println("highways:     " + highways);
					System.out.println("nodes:        " + nodes);
					System.out.println("highwaynodes: " + highwayNodes.size());
					System.out.println("filesize:     " + fileSize);
				}
			};
			
			OsmUtils.readFromOsm(is, s);
		}
	}

}
