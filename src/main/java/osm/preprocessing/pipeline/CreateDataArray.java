package osm.preprocessing.pipeline;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;

import osm.map.Graph;
import osm.preprocessing.DataProcessor;
import osm.preprocessing.PipelineParts.PipelinePaths;
import osmlab.io.AbstractHighwaySink;
import osmlab.io.SimpleSink;
import osmlab.sink.ByteUtils;
import osmlab.sink.FormatConstants;
import osmlab.sink.GeoUtils;
import osmlab.sink.OsmUtils;
import osmlab.sink.OsmUtils.TriConsumer;

public class CreateDataArray extends DataProcessor {

	public CreateDataArray(PipelinePaths paths,
			TriConsumer<String, Integer, Integer> progressHandler) {
		super(paths, progressHandler);
	}

	@Override
	public void process() throws IOException {
		try (DataInputStream highwayNodesSortedSizes = new DataInputStream(
				new FileInputStream(paths.HIGHWAY_NODES_SORTED_SIZE));
				DataInputStream highwayNodesSorted = new DataInputStream(
						new BufferedInputStream(new FileInputStream(
								paths.HIGHWAY_NODES_SORTED)));
				DataInputStream offsetArrayRaw = new DataInputStream(
						new BufferedInputStream(new FileInputStream(
								paths.OFFSET_ARRAY_RAW)));
				DataInputStream dataArraySize = new DataInputStream(
						new BufferedInputStream(new FileInputStream(
								paths.DATA_ARRAY_SIZE)));
				DataInputStream aggregateOffsetArrayRaw = new DataInputStream(
						new BufferedInputStream(new FileInputStream(
								paths.AGGREGATE_OFFSET_ARRAY_RAW)));
				DataInputStream aggregateDataArraySize = new DataInputStream(
						new BufferedInputStream(new FileInputStream(
								paths.AGGREGATE_DATA_ARRAY_SIZE)));
				InputStream is = new FileInputStream(paths.SOURCE_FILE);

				ObjectOutputStream dataArrayStream = new ObjectOutputStream(
						new BufferedOutputStream(new FileOutputStream(
								paths.DATA_ARRAY)));
				ObjectOutputStream offsetArrayStream = new ObjectOutputStream(
						new BufferedOutputStream(new FileOutputStream(
								paths.OFFSET_ARRAY)));

				ObjectOutputStream aggregateDataArrayStream = new ObjectOutputStream(
						new BufferedOutputStream(new FileOutputStream(
								paths.AGGREGATE_DATA_ARRAY)));
				ObjectOutputStream aggregateOffsetArrayStream = new ObjectOutputStream(
						new BufferedOutputStream(new FileOutputStream(
								paths.AGGREGATE_OFFSET_ARRAY)));
				
		) {

			int nodeCount = (int) highwayNodesSortedSizes.readInt();
			int dataSize = dataArraySize.readInt();

			long[] allNodes = new long[nodeCount];
			int[] offsetArray = new int[nodeCount];
			int[] data = new int[dataSize];

			int aggregateDataSize = aggregateDataArraySize.readInt();
			int[] aggregateOffsetArray = new int[nodeCount];
			int[] aggregateData = new int[aggregateDataSize];

			for (int i = 0; i < nodeCount; i++) {
				allNodes[i] = highwayNodesSorted.readLong();
				offsetArray[i] = offsetArrayRaw.readInt();

				if (i % (nodeCount / 100) == 0) {
					progressHandler.accept("Reading raw Node IDs and offsets",
							i, nodeCount);
				}
			}

			for (int i = 0; i < nodeCount; i++) {
				aggregateOffsetArray[i] = aggregateOffsetArrayRaw.readInt();

				if (i % (nodeCount / 100) == 0) {
					progressHandler.accept("Reading aggregate offsets", i,
							nodeCount);
				}
			}

			SimpleSink s = new AbstractHighwaySink() {

				private int highways = 0;
				private int nodes;

				private final int expectedHighways = (int) (CreateDataArray.this.sourceFileSize * FormatConstants.highwaysPerByte);
				private final int expectedNodes = (int) (CreateDataArray.this.sourceFileSize * FormatConstants.nodesPerByte);
				private final int expectedHighwaysAndNodes = expectedHighways
						+ expectedNodes;

				@Override
				public void handleHighway(Way way, HighwayInfos infos) {
					highways++;

					if ((highways + nodes) % (expectedHighwaysAndNodes / 100) == 0) {
						progressHandler
								.accept("Filling coordinates and link information into data array",
										(int) (highways + nodes),
										expectedHighwaysAndNodes);
					}
					List<WayNode> wayNodes = way.getWayNodes();
					handleNormal(wayNodes, infos);
					handleAggregate(wayNodes, infos);

					//
					//

				}

				private void handleAggregate(List<WayNode> wayNodes,
						HighwayInfos infos) {
					// remeber aggregate way, first -> last
					long aggregateStartNode = wayNodes.get(0).getNodeId();
					long aggregateEndNode = wayNodes.get(wayNodes.size() - 1)
							.getNodeId();

					int indexOfAggregateStartNode = Arrays.binarySearch(
							allNodes, aggregateStartNode);
					int indexOfAggregateEndNode = Arrays.binarySearch(allNodes,
							aggregateEndNode);

					addAggregateEdgeFromTo(indexOfAggregateStartNode,
							indexOfAggregateEndNode, infos);
					if (!infos.Oneway) {
						addAggregateEdgeFromTo(indexOfAggregateEndNode,
								indexOfAggregateStartNode, infos);
					}
				}

				private void handleNormal(List<WayNode> wayNodes,
						HighwayInfos infos) {

					// remember one link for each direction
					for (int i = 1; i < wayNodes.size(); i++) {

						long firstNode = wayNodes.get(i - 1).getNodeId();
						long secondNode = wayNodes.get(i).getNodeId();

						int indexOfFirstNode = Arrays.binarySearch(allNodes,
								firstNode);
						int indexOfSecondNode = Arrays.binarySearch(allNodes,
								secondNode);

						addEdgeFromTo(indexOfFirstNode, indexOfSecondNode,
								infos);
						if (!infos.Oneway) {
							addEdgeFromTo(indexOfSecondNode, indexOfFirstNode,
									infos);
						}

					}
				}

				private void addAggregateEdgeFromTo(
						int indexOfAggregateStartNode,
						int indexOfAggregateEndNode, HighwayInfos infos) {
					int offset = aggregateOffsetArray[indexOfAggregateStartNode];
					offset += FormatConstants.CONSTANT_NODESIZE;
					// Skip LAT / LON. We want to enter the edge only
					// There may be neighbours already, skip to first 0-spot
					// (assuming no neighbour yet defaults to 0 in dataArray)
					while (aggregateData[offset] != 0) {
						offset += FormatConstants.CONSTANT_EDGESIZE;
					}
					// fill in data of connection - target id, speed and
					// pedestrian yes/no
					int edge = ByteUtils.encodeEdge(indexOfAggregateEndNode,
							infos.pedestrian, infos.MaxSpeed);
					aggregateData[offset] = edge;
				}

				private void addEdgeFromTo(int indexOfFirstNode,
						int indexOfSecondNode, HighwayInfos infos) {
					int offset = offsetArray[indexOfFirstNode];
					offset += FormatConstants.CONSTANT_NODESIZE;
					// Skip LAT / LON. We want to enter the edge only
					// There may be neighbours already, skip to first 0-spot
					// (assuming no neighbour yet defaults to 0 in dataArray)
					while (data[offset] != 0) {
						offset += FormatConstants.CONSTANT_EDGESIZE;
					}
					// fill in data of connection - target id, speed and
					// pedestrian yes/no
					int edge = ByteUtils.encodeEdge(indexOfSecondNode,
							infos.pedestrian, infos.MaxSpeed);
					data[offset] = edge;
				}

				@Override
				public void handleNode(Node node) {
					nodes++;

					if ((highways + nodes) % (expectedHighwaysAndNodes / 100) == 0) {
						progressHandler
								.accept("Filling coordinates and link information into data array",
										(int) (highways + nodes),
										expectedHighwaysAndNodes);
					}
					handleNormal(node);
					handleAggregate(node);
					

				}

				private void handleAggregate(Node node) {
					// if the node is part of a highway, we will find it with a
					// binary search. in that case, we add the coordinates of it
					// to
					// data array. otherwise, its no node that is part of a
					// highway and we skip it
					int indexOfNode = Arrays.binarySearch(allNodes,
							node.getId());
					// if true, this is a node which is part of a highway
					if (indexOfNode > 0) {
						int offset = aggregateOffsetArray[indexOfNode];
						// First int is lat, second int is lon
						aggregateData[offset] = Float.floatToRawIntBits((float) node
								.getLatitude());
						aggregateData[offset + 1] = Float.floatToRawIntBits((float) node
								.getLongitude());
					}			
				}

				private void handleNormal(Node node) {
					// if the node is part of a highway, we will find it with a
					// binary search. in that case, we add the coordinates of it
					// to
					// data array. otherwise, its no node that is part of a
					// highway and we skip it
					int indexOfNode = Arrays.binarySearch(allNodes,
							node.getId());
					// if true, this is a node which is part of a highway
					if (indexOfNode > 0) {
						int offset = offsetArray[indexOfNode];
						// First int is lat, second int is lon
						data[offset] = Float.floatToRawIntBits((float) node
								.getLatitude());
						data[offset + 1] = Float.floatToRawIntBits((float) node
								.getLongitude());
					}		
				}

				@Override
				public void complete() {

					calculateDistances();
					// Write our final datastructures to disk as serialized java
					// arrays - offsetarray and data array.
					try {
						progressHandler.accept("Writing data array to disc",
								-1, -1);
						dataArrayStream.writeObject(data);
						progressHandler.accept("Writing offset array to disc",
								-1, -1);
						offsetArrayStream.writeObject(offsetArray);
						
						
						progressHandler.accept("Writing aggregate data array to disc",
								-1, -1);
						aggregateDataArrayStream.writeObject(aggregateData);
						progressHandler.accept("Writing aggregate offset array to disc",
								-1, -1);
						aggregateOffsetArrayStream.writeObject(aggregateOffsetArray);
						
					} catch (IOException e) {
						e.printStackTrace();
					}

				}

				private void calculateDistances() {

					Graph graph = new Graph(data, offsetArray, aggregateData, aggregateOffsetArray);

					for (int nodeId = 0; nodeId < nodeCount; nodeId++) {

						if (nodeId % (nodeCount / 100) == 0) {
							progressHandler.accept(
									"Calculating distances between nodes",
									nodeId, nodeCount);
						}

						int offsetOfNode = offsetArray[nodeId];
						AtomicInteger offsetOfDistance = new AtomicInteger(
								offsetOfNode
										+ FormatConstants.CONSTANT_NODESIZE + 1);

						graph.forEachEdgeOf(
								nodeId,
								(node, neighbour) -> {
									float distance = GeoUtils.distFrom(
											graph.latOf(node),
											graph.lonOf(node),
											graph.latOf(neighbour),
											graph.lonOf(neighbour));
									data[offsetOfDistance.intValue()] = Float
											.floatToRawIntBits(distance);

									offsetOfDistance
											.addAndGet(FormatConstants.CONSTANT_EDGESIZE);
								});
					}
				}
			};

			OsmUtils.readFromOsm(s, is);
		}
	}

}
