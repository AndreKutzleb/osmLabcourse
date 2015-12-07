package osmlab.io;
import java.io.IOException;

import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;

import com.carrotsearch.hppc.LongOpenHashSet;

public class HighwayNodeSegmenter extends SimpleSink {

	private final Cartographer cartographer;
	private final LongOpenHashSet highwayNodes;

	public HighwayNodeSegmenter(Cartographer cartographer,
			LongOpenHashSet highwayNodes) {
		this.cartographer = cartographer;
		this.highwayNodes = highwayNodes;
	}

	@Override
	public void process(EntityContainer entityContainer) {
		Entity entity = entityContainer.getEntity();

		if (entity instanceof Node) {

			Node node = (Node) entity;

			if (highwayNodes.contains(node.getId())) {
				try {
					cartographer.writeSimpleNode(node);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

		}
	}
}