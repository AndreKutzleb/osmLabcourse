package osm.preprocessing;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import osm.preprocessing.pipeline.CreateDataArray;
import osm.preprocessing.pipeline.CreateOffsetArray;
import osm.preprocessing.pipeline.ExtractHighwayNodes;
import osm.preprocessing.pipeline.FilterDuplicatesAndSortHighwayNodes;

public class PipelineParts {

	public static class PipelinePaths {

		public final String SOURCE_FILE;
		public final String HIGHWAY_NODES_RAW;
		public final String HIGHWAY_NODES_RAW_DATA;
		public final String HIGHWAY_NODES_SORTED;
		public final String HIGHWAY_NODES_SORTED_DATA;
		public final String OFFSET_ARRAY_RAW;
		
		public PipelinePaths(String sourceFilePath) {
			
			File sourceFile = new File(sourceFilePath);
			String simpleName = sourceFile.getName();
			String nameWithoutType = simpleName.substring(0, simpleName.indexOf('.'));
			String mainDataFolderPath = "data";
			String dataFolderPath = mainDataFolderPath + File.separator + nameWithoutType;
			File dataFolder = new File(dataFolderPath);
			dataFolder.mkdirs();

			
			SOURCE_FILE = sourceFilePath;
			HIGHWAY_NODES_RAW = dataFolder + File.separator + "highwaynodes_raw.data";
			HIGHWAY_NODES_RAW_DATA = dataFolder + File.separator + "highwaynodes_raw.sizes";
			HIGHWAY_NODES_SORTED = dataFolder + File.separator + "highwaynodes_sorted.data";
			HIGHWAY_NODES_SORTED_DATA = dataFolder + File.separator + "highwaynodes_sorted.sizes";
			OFFSET_ARRAY_RAW = dataFolder + File.separator + "offsetarray_raw.data";
		}
	}
	
	private List<Supplier<DataProcessor>> pipelineSteps = new ArrayList<Supplier<DataProcessor>>();

	public PipelineParts(String sourceFilePath) {
		
		
		PipelinePaths paths = new PipelinePaths(sourceFilePath);
		
		pipelineSteps.add(() -> new ExtractHighwayNodes(paths));
		pipelineSteps.add(() -> new FilterDuplicatesAndSortHighwayNodes(paths));
		pipelineSteps.add(() -> new CreateOffsetArray());
		pipelineSteps.add(() -> new CreateDataArray());
	}
}
