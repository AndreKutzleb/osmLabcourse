package osmlab.sink;

import java.io.InputStream;
import java.util.Objects;

import org.openstreetmap.osmosis.core.task.v0_6.RunnableSource;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;

public class OsmUtils {

	public static void readFromOsm(InputStream is, Sink sink) {
		RunnableSource reader = new crosby.binary.osmosis.OsmosisReader(is);

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
