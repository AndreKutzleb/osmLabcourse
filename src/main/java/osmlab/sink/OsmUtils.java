package osmlab.sink;

import java.io.InputStream;
import java.util.Map;
import java.util.Objects;

import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.task.v0_6.RunnableSource;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;

public class OsmUtils {

	public static void readFromOsm(Sink sink, InputStream... is) {
	
		Sink relay = new Sink() {
			
			@Override
			public void process(EntityContainer entityContainer) {
				sink.process(entityContainer);
			}

			@Override
			public void initialize(Map<String, Object> metaData) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void complete() {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void release() {
				// TODO Auto-generated method stub
				
			}
		};
		
		for(InputStream stream : is) {
			
		RunnableSource reader = new crosby.binary.osmosis.OsmosisReader(stream);

		reader.setSink(sink);

		Thread readerThread = new Thread(reader);
		readerThread.start();

		while (readerThread.isAlive()) {
			try {
				readerThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		}
		sink.complete();
		sink.release();
		}
	
	@FunctionalInterface
	public interface TriConsumer<T, U, V> {
	  public void accept(T t, U u, V v);

	  public default TriConsumer<T, U, V> andThen(TriConsumer<? super T, ? super U, ? super V> after) {
	    Objects.requireNonNull(after);
	    return (a, b, c) -> {
	      accept(a, b, c);
	      after.accept(a, b, c);
	    };
	  }
	}
}
