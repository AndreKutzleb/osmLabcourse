package osm.preprocessing;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.function.IntConsumer;

import osm.preprocessing.PipelineParts.PipelinePaths;

public abstract class DataProcessor {
	
	protected final PipelinePaths paths;
	private IntConsumer progressHandler;

	public DataProcessor(PipelinePaths paths, IntConsumer progressHandler) {
		this.paths = paths;
		this.progressHandler = progressHandler;
	}

	public abstract void process() throws IOException;
	
	
}
