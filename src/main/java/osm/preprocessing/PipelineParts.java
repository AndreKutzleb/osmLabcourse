package osm.preprocessing;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import osm.preprocessing.pipeline.Analyzer;
import osm.preprocessing.pipeline.CreateDataArray;
import osm.preprocessing.pipeline.CreateOffsetArray;
import osm.preprocessing.pipeline.ExtractHighwayNodes;
import osm.preprocessing.pipeline.FilterDuplicatesAndSortHighwayNodes;
import osmlab.sink.OsmUtils.TriConsumer;

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
		public final String AGGREGATE_OFFSET_ARRAY_RAW;
		public final String AGGREGATE_DATA_ARRAY_SIZE;
		public final String AGGREGATE_DATA_ARRAY;
		public final String AGGREGATE_OFFSET_ARRAY;
		
		public PipelinePaths(String sourceFilePath) {
			
			File sourceFile = new File(sourceFilePath);
			String simpleName = sourceFile.getName();
			String nameWithoutType = simpleName.substring(0, simpleName.indexOf('.'));
			String mainDataFolderPath = "data";
			String dataFolderPath = mainDataFolderPath + File.separator + nameWithoutType;
			File dataFolder = new File(dataFolderPath);
			dataFolder.mkdirs();

			String base = dataFolderPath + File.separator;
			
			SOURCE_FILE = sourceFilePath;
			HIGHWAY_NODES_RAW = 		 base + "highwaynodes_raw.data";
			HIGHWAY_NODES_RAW_SIZE = 	 base + "highwaynodes_raw.size";
			HIGHWAY_NODES_SORTED =	 	 base + "highwaynodes_sorted.data";
			HIGHWAY_NODES_SORTED_SIZE =  base + "highwaynodes_sorted.size";
			OFFSET_ARRAY_RAW = 			 base + "offsetarray_raw.data";
			OFFSET_ARRAY = 				 base + "offsetarray.data";
			DATA_ARRAY_SIZE = 			 base + "dataarray.size";
			DATA_ARRAY = 				 base + "dataarray.data";
			AGGREGATE_OFFSET_ARRAY_RAW = base + "aggregate_offsetarray_raw.data";
			AGGREGATE_DATA_ARRAY_SIZE =  base + "aggregate_dataarray.size";
			AGGREGATE_OFFSET_ARRAY = 	 base + "aggregate_offsetarray.data";
			AGGREGATE_DATA_ARRAY =		 base + "aggregate_dataarray.data";
			
		}
	}
	
	private List<DataProcessor> pipelineSteps = new ArrayList<>();

	public PipelineParts(String sourceFilePath,TriConsumer<String, Integer, Integer> progressHandler) {
		
		
		PipelinePaths paths = new PipelinePaths(sourceFilePath);
		
		pipelineSteps.add(new Analyzer(paths, progressHandler));
		pipelineSteps.add(new ExtractHighwayNodes(paths,progressHandler));
		pipelineSteps.add(new FilterDuplicatesAndSortHighwayNodes(paths,progressHandler));
		pipelineSteps.add(new CreateOffsetArray(paths,progressHandler));
		pipelineSteps.add(new CreateDataArray(paths,progressHandler));
	}
	
	public List<DataProcessor> getPipelineSteps() {
		return pipelineSteps;
	}
}
