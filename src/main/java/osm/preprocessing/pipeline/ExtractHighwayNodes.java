package osm.preprocessing.pipeline;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.IntConsumer;

import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;

import osm.preprocessing.DataProcessor;
import osm.preprocessing.PipelineParts.PipelinePaths;
import osmlab.io.AbstractHighwaySink;
import osmlab.sink.OsmUtils;

public class ExtractHighwayNodes extends DataProcessor {

	public ExtractHighwayNodes(PipelinePaths paths, IntConsumer progressHandler) {
		super(paths,progressHandler);
	}

	@Override
	public void process() throws IOException {

		try (OutputStream os = new FileOutputStream(paths.HIGHWAY_NODES_RAW);
				OutputStream osMeta = new FileOutputStream(
						paths.HIGHWAY_NODES_RAW_DATA);
				InputStream is = new FileInputStream(paths.SOURCE_FILE);) {

			HighwaySink sink = new HighwaySink(os, osMeta);
			OsmUtils.readFromOsm(is, sink);
		}
	}

	class HighwaySink extends AbstractHighwaySink {

		int nodes = 0;

		private final DataOutputStream os;
		private final DataOutputStream osMeta;

		public HighwaySink(OutputStream os, OutputStream osMeta) {
			this.osMeta = new DataOutputStream(osMeta);
			this.os = new DataOutputStream(new BufferedOutputStream(os));
		}

		@Override
		public void handleHighway(Way way) {
			for (WayNode n : way.getWayNodes()) {
				nodes++;
				try {
					os.writeLong(n.getNodeId());
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		@Override
		public void complete() {
			try {
				osMeta.writeInt(nodes);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
