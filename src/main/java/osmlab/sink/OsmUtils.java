package osmlab.sink;

import java.io.InputStream;

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
}
