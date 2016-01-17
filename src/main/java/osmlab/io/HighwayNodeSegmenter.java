package osmlab.io;

import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;

public class HighwayNodeSegmenter extends SimpleSink {

	@Override
	public void process(EntityContainer entityContainer) {
		// TODO Auto-generated method stub
		
	}

//	long nodesProcessed = 0; 
//	private final Cartographer cartographer;
//	private final LongSet highwayNodes;
//
//	public HighwayNodeSegmenter(Cartographer cartographer,
//			LongSet highwayNodes) {
//		this.cartographer = cartographer;
//		this.highwayNodes = highwayNodes;
//	}
//
//	@Override
//	public void process(EntityContainer entityContainer) {
//		Entity entity = entityContainer.getEntity();
//
//		if (entity instanceof Node) {
//
//			Node node = (Node) entity;
//
//			if (highwayNodes.contains(node.getId())) {
//				try {
//					nodesProcessed++;
//					if(nodesProcessed % 10000 == 0) {
//						System.out.println(nodesProcessed +" nodes Processed");
//					}
//					cartographer.writeSimpleNode(node);
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//			}
//
//		}
//	}
}