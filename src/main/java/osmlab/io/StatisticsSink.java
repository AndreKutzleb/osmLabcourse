package osmlab.io;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;

import com.carrotsearch.hppc.LongOpenHashSet;

public class StatisticsSink extends SimpleSink {

	private final LongOpenHashSet highwayNodes = new LongOpenHashSet();
	private long highwayCount;
	private long edgeCount;

	public StatisticsSink() {
	}

	@Override
	public void process(EntityContainer entityContainer) {
		Entity entity = entityContainer.getEntity();

		if (entity instanceof Way) {

			Way way = (Way) entity;

			way.getTags().stream()
					.filter(tag -> tag.getKey().equals("highway")).findAny()
					.ifPresent(tag -> handleHighway(way, tag.getValue()));

		}
	}

	@Override
	public void complete() {
		
		long directedEdgeCount = edgeCount*2;
		double avgOutgoingEdgesPerNode = directedEdgeCount / (double) highwayNodes.size();
		
		String f = "%-30s: %s\n";
		System.out.printf(f,"Highways", highwayCount);
		System.out.printf(f,"Unique highway nodes", highwayNodes.size());
		System.out.printf(f,"Directed edges",directedEdgeCount);
		System.out.printf(f,"Avg. outgoing edges", String.format("%f", avgOutgoingEdgesPerNode));

	}
	
	void handleHighway(Way way, String highwayType) {
		highwayCount++;
		for (WayNode n : way.getWayNodes()) {
			highwayNodes.add(n.getNodeId());
		}
		edgeCount+=(way.getWayNodes().size()-1);
		
		
	}

}