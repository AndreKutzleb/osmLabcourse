package osm.preprocessing.pipeline;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;

import osm.preprocessing.DataProcessor;
import osm.preprocessing.PipelineParts.PipelinePaths;
import osmlab.io.AbstractHighwaySink;
import osmlab.sink.FormatConstants;
import osmlab.sink.OsmUtils;
import osmlab.sink.OsmUtils.TriConsumer;

public class ExtractHighwayNodes extends DataProcessor {

	public ExtractHighwayNodes(PipelinePaths paths,
			TriConsumer<String, Integer, Integer> progressHandler) {
		super(paths, progressHandler);
	}

	@Override
	public void process() throws IOException {

		try (OutputStream os = new BufferedOutputStream(new FileOutputStream(
				paths.HIGHWAY_NODES_RAW));
				OutputStream osMeta = new BufferedOutputStream(
						new FileOutputStream(paths.HIGHWAY_NODES_RAW_SIZE));
				InputStream is = new BufferedInputStream(new FileInputStream(
						paths.SOURCE_FILE));) {

			HighwaySink sink = new HighwaySink(os, osMeta);
			OsmUtils.readFromOsm(sink, is);
		}
	}

	class HighwaySink extends AbstractHighwaySink {

		int highways = 0;
		int nodes = 0;
		int expectedHighways = (int) (ExtractHighwayNodes.this.sourceFileSize * FormatConstants.highwaysPerByte);
		private final DataOutputStream os;
		private final DataOutputStream osMeta;

		public HighwaySink(OutputStream os, OutputStream osMeta) {
			this.osMeta = new DataOutputStream(osMeta);
			this.os = new DataOutputStream(new BufferedOutputStream(os));
		}

		@Override
		public void handleHighway(Way way, HighwayInfos infos) {
			highways++;
			// 100 updates at most
			if (highways % (expectedHighways / 100) == 0) {
				progressHandler.accept("Extracting Node IDs from Source file",
						highways, expectedHighways);
			}

			for (WayNode n : way.getWayNodes()) {
				try {
					os.writeLong(n.getNodeId());
					nodes++;
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		@Override
		public void complete() {
			try {
				osMeta.writeInt(nodes);
				os.flush();
				osMeta.flush();

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
