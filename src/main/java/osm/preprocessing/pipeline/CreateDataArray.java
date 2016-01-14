package osm.preprocessing.pipeline;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.IntConsumer;

import osm.preprocessing.DataProcessor;
import osm.preprocessing.PipelineParts.PipelinePaths;

public class CreateDataArray extends DataProcessor{

	public CreateDataArray(PipelinePaths paths, IntConsumer progressHandler) {
		super(paths, progressHandler);
	}

	@Override
	public void process() throws IOException {
		try (DataInputStream highwayNodesSortedSizes = new DataInputStream(new FileInputStream(paths.HIGHWAY_NODES_SORTED_DATA));
				DataInputStream highwayNodesSorted = new DataInputStream(new BufferedInputStream(new FileInputStream(paths.HIGHWAY_NODES_SORTED)));
				InputStream is = new FileInputStream(paths.SOURCE_FILE);
				DataOutputStream offsetArrayRaw = new DataOutputStream( new BufferedOutputStream(new FileOutputStream(paths.OFFSET_ARRAY_RAW)));
				) {
	}

}
