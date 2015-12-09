package osmlab.io;
import java.io.IOException;
import java.io.OutputStream;

import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;

import osmlab.proto.OsmLight;
import osmlab.proto.OsmLight.SimpleWay;
import osmlab.proto.OsmLight.SimpleWay.Builder;

import com.carrotsearch.hppc.LongOpenHashSet;


public class HighwayNodeSink extends SimpleSink {

	long nodesProcessed = 0;
	private final OutputStream os;

	private final LongOpenHashSet highwayNodes;

	public HighwayNodeSink(OutputStream os, LongOpenHashSet highwayNodes) {
		this.os = os;
		this.highwayNodes = highwayNodes;
	}

	@Override
	public void process(EntityContainer entityContainer) {
		Entity entity = entityContainer.getEntity();

		if (entity instanceof Way) {

			Way way = (Way) entity;

			way.getTags().stream()
					.filter(tag -> tag.getKey().equals("highway"))
					.findAny()
					.ifPresent(tag -> handleHighway(way, tag.getValue()));

		}
	}

	void handleHighway(Way way, String highwayType) {
		OsmLight.SimpleWay.Builder simpleWay = OsmLight.SimpleWay
				.newBuilder();
		simpleWay.setId(way.getId());
		for (WayNode n : way.getWayNodes()) {
			simpleWay.addNode(n.getNodeId());
			highwayNodes.add(n.getNodeId());
			nodesProcessed++;
			if(nodesProcessed % 10000 == 0) {
				System.out.println(nodesProcessed +" nodes Processed");
			}
		}

		// org.openstreetmap.osmosis.osmbinary.Osmformat.Node.Builder
		// builder =
		// org.openstreetmap.osmosis.osmbinary.Osmformat.Node.newBuilder();
		// builder.setId(way)

		try {
			simpleWay.build().writeDelimitedTo(os);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}