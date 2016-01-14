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
		public final String HIGHWAY_NODES_RAW_SIZE;
		public final String HIGHWAY_NODES_SORTED;
		public final String HIGHWAY_NODES_SORTED_SIZE;
		public final String OFFSET_ARRAY_RAW;
		public final String OFFSET_ARRAY;
		public final String DATA_ARRAY_SIZE;
		public final String DATA_ARRAY;
		
		public PipelinePaths(String sourceFilePath) {
			
			File sourceFile = new File(sourceFilePath);
			String simpleName = sourceFile.getName();
			String nameWithoutType = simpleName.substring(0, simpleName.indexOf('.'));
			String mainDataFolderPath = "data";
			String dataFolderPath = mainDataFolderPath + File.separator + nameWithoutType;
			File dataFolder = new File(dataFolderPath);
			dataFolder.mkdirs();

			String f = dataFolderPath + File.separator;
			
			SOURCE_FILE = sourceFilePath;
			HIGHWAY_NODES_RAW = 		f + "highwaynodes_raw.data";
			HIGHWAY_NODES_RAW_SIZE = 	f + "highwaynodes_raw.size";
			HIGHWAY_NODES_SORTED =	 	f + "highwaynodes_sorted.data";
			HIGHWAY_NODES_SORTED_SIZE = f + "highwaynodes_sorted.size";
			OFFSET_ARRAY_RAW = 			f + "offsetarray_raw.data";
			OFFSET_ARRAY = 				f + "offsetarray.data";
			DATA_ARRAY_SIZE = 			f + "dataarray.size";
			DATA_ARRAY = 				f + "dataarray.data";
			
			//OFFSET_ARRAY_RAW_SIZE = dataFolder + File.separator + "offsetarray_raw.size";

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
