package osmlab.io;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;


public abstract class AbstractHighwaySink extends SimpleSink {

	@Override
	public void process(EntityContainer entityContainer) {
		Entity entity = entityContainer.getEntity();
		if (entity instanceof Way) {

			Way way = (Way) entity;

			if (way.getWayNodes().size() > 1) {

				way.getTags().stream()
						.filter(tag -> tag.getKey().equals("highway"))
						.findAny().ifPresent(tag -> handleHighway(way));
			}

		} else if (entity instanceof Node) {
			Node node = (Node) entity;
			handleNode(node);
		}
	}

	public void handleHighway(Way way) {};

	public void handleNode(Node node) {};

}