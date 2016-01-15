package osm.preprocessing;

import java.io.File;
import java.io.IOException;
import java.util.function.BiConsumer;

import osm.preprocessing.PipelineParts.PipelinePaths;
import osmlab.sink.OsmUtils.TriConsumer;

public abstract class DataProcessor {
	
	protected final PipelinePaths paths;
	protected final TriConsumer<String, Integer, Integer> progressHandler;
	protected final long sourceFileSize;

	public DataProcessor(PipelinePaths paths, TriConsumer<String, Integer, Integer> progressHandler) {
		this.paths = paths;
		this.progressHandler = progressHandler;
		this.sourceFileSize = new File(paths.SOURCE_FILE).length();
	}

	public abstract void process() throws IOException;
	
	
}
