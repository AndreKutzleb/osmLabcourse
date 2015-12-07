package osmlab.io;
import java.util.Map;

import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;


public abstract class SimpleSink implements Sink {

	@Override
	public void initialize(Map<String, Object> metaData) {		
	}

	@Override
	public void complete() {
	
	}

	@Override
	public void release() {
	}

	@Override
	public abstract void process(EntityContainer entityContainer);
	
	

}
