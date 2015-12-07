package osmlab;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.openstreetmap.osmosis.core.task.v0_6.RunnableSource;

import osmlab.io.Cartographer;
import osmlab.io.Cartographer.ConnectionDetails;
import osmlab.io.Cartographer.SimpleNode;
import osmlab.io.HighwayNodeSegmenter;
import osmlab.io.HighwayNodeSink;
import osmlab.proto.OsmLight.SimpleWay;
import osmlab.sink.ByteUtils;

import com.carrotsearch.hppc.LongOpenHashSet;
import com.google.protobuf.Parser;

public class DataPreparer {

	public void prepareData(File inputFile) throws IOException {

		String fileName = inputFile.getName().substring(0,
				inputFile.getName().indexOf('.'));
		new File(fileName).mkdir();
		String waysFile = fileName + File.separator + fileName + ".ways.pbf";

		LongOpenHashSet highwayNodes = new LongOpenHashSet();

		// set of all highwaynodes and filtering and saving all ways in own
		// format
		cacheHighwayNodesAndWriteHighwaysToDisk(waysFile, highwayNodes,
				new FileInputStream(inputFile));

		// reading all nodes, filtering those who are in the set of highway
		// nodes, and saving them in segmented format.
		segmentAndSaveAllHighwayNodesWithCoordinates(fileName, highwayNodes,
				inputFile);

		// sort the nodes by id in each segment file
		long[][] segmentedSortedNodes = sortSegmentFiles(fileName);

		createSegmentOverlappingNodeFile(segmentedSortedNodes, waysFile,
				fileName);
	}

	private void createSegmentOverlappingNodeFile(
			long[][] segmentedSortedNodes, String waysFile, String mapName)
			throws IOException {
		try (Cartographer cartographer = new Cartographer(mapName)) {

			AtomicInteger nodesHandled = new AtomicInteger(0);

			parseProtobuf(new BufferedInputStream(new FileInputStream(new File(
					waysFile))), SimpleWay.PARSER, (way) -> {
				handleWay(way, segmentedSortedNodes, cartographer);
				if (nodesHandled.incrementAndGet() % 1000 == 0) {
					System.out.println(nodesHandled + " processed.");
				}
			});
		}

	}

	private void handleWay(SimpleWay way, long[][] segmentedSortedNodes,
			Cartographer cartographer) {
		if (way.getNodeCount() > 1) {
			NodePos prevNode = findNodeData(way.getNode(0),
					segmentedSortedNodes);
			for (int i = 0; i < way.getNodeCount(); i++) {
				long id = way.getNodeList().get(i);
				NodePos curNode = findNodeData(id, segmentedSortedNodes);

				// TODO speed, pedestrian, car
				ConnectionDetails prevToNow = new ConnectionDetails(true, true,
						(byte) 30, prevNode.latLonBase != curNode.latLonBase,
						curNode.index, curNode.latLonBase);
				ConnectionDetails nowToPrev = new ConnectionDetails(true, true,
						(byte) 30, prevNode.latLonBase != curNode.latLonBase,
						prevNode.index, prevNode.latLonBase);

				try {
					cartographer.writePairwiseConnection(prevNode, prevToNow);
					cartographer.writePairwiseConnection(curNode, nowToPrev);
				} catch (IOException e) {
					e.printStackTrace();
				}

			}
		}

	}

	public static class NodePos {
		public final int latLonBase;
		public final int index;
		public final int latOffset;
		public final int lonOffset;

		public NodePos(int latOffset, int lonOffset, int latLonBase, int index) {
			this.latOffset = latOffset;
			this.lonOffset = lonOffset;
			this.latLonBase = latLonBase;
			this.index = index;
		}

		public static NodePos parseFrom(long[] nodes, int middle, int latLonBase) {
			int lat = (int) (nodes[middle + 1] >>> 32);
			int lon = (int) nodes[middle + 1];
			return new NodePos(lat, lon, latLonBase, middle);
		}
	}

	// search for 64 bit id, get index of id in segment, as well as lat and lon.
	private NodePos findNodeData(long id, long[][] segmentedSortedNodes) {
		for (int i = 0; i < segmentedSortedNodes.length; i++) {
			long[] nodes = segmentedSortedNodes[i];
			if (nodes != null) {
				NodePos nodePos = binarySearch(id, nodes, i);
				if (nodePos != null) {
					return nodePos;
				}
			}
		}

		throw new IllegalStateException("data for node could not be found");
	}

	// 10-0

	public NodePos binarySearch(long id, long[] nodes, int latLonBase) {

		if (id > nodes[nodes.length - 2] || id < nodes[0]) {
			return null;
		}

		int low = 0;
		int high = nodes.length - 1;

		while (high >= low) {
			int middle = ((low + high) / 2) * 2;
			if (nodes[middle] == id) {
				return NodePos.parseFrom(nodes, middle, latLonBase);
			}
			if (nodes[middle] < id) {
				low = middle + 2;
			}
			if (nodes[middle] > id) {
				high = middle - 2;
			}
		}
		return null;
	}

	private long[][] sortSegmentFiles(String fileName)
			throws FileNotFoundException, IOException {
		String highwayNodesFolderName = fileName + File.separator
				+ "highwayNodes";

		long[][] segmentedSortedNodeIds = new long[92520][];

		File[] files = new File(highwayNodesFolderName).listFiles(f -> f
				.getName().endsWith(".nodes"));
		for (File f : files) {
			String rawFileName = f.getName().substring(0,
					f.getName().indexOf('.'));
			int latLon = Integer.parseInt(rawFileName);

			List<Cartographer.SimpleNode> nodes = new ArrayList<>();
			try (DataInputStream dis = new DataInputStream(
					new BufferedInputStream(new FileInputStream(f)));) {

				while (dis.available() > 0) {
					Cartographer.SimpleNode node = Cartographer.SimpleNode
							.parseFromStream(dis);
					nodes.add(node);
				}
			}
			Collections.sort(nodes);
			long[] sortedIds = new long[nodes.size() * 2];
			for (int i = 0; i < nodes.size(); i++) {
				SimpleNode node = nodes.get(i);
				sortedIds[i * 2] = node.id;
				sortedIds[(i * 2) + 1] = (Short.toUnsignedLong(node.lat) << 32)
						| (Short.toUnsignedLong(node.lon));

			}

			segmentedSortedNodeIds[latLon] = sortedIds;
		}
		return segmentedSortedNodeIds;
	}

	private void cacheHighwayNodesAndWriteHighwaysToDisk(String waysFile,
			LongOpenHashSet highwayNodes, InputStream inputStream)
			throws IOException {
		try (FileOutputStream waysWriter = new FileOutputStream(new File(
				waysFile));) {

			HighwayNodeSink hNodeSink = new HighwayNodeSink(waysWriter,
					highwayNodes);

			RunnableSource reader;

			reader = new crosby.binary.osmosis.OsmosisReader(inputStream);

			reader.setSink(hNodeSink);

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

	private void segmentAndSaveAllHighwayNodesWithCoordinates(String mapName,
			LongOpenHashSet highwayNodes, File inputFile) throws IOException {

		try (Cartographer cartographer = new Cartographer(mapName)) {

			HighwayNodeSegmenter hNodeSink = new HighwayNodeSegmenter(
					cartographer, highwayNodes);

			RunnableSource reader;

			reader = new crosby.binary.osmosis.OsmosisReader(
					new FileInputStream(inputFile));

			reader.setSink(hNodeSink);

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

	// public static void merge(File ways, File nodes, File onlyRequiredNodes)
	// throws FileNotFoundException, IOException {
	// try (FileInputStream waysReader = new FileInputStream(ways);
	// FileInputStream nodesReader = new FileInputStream(nodes);
	// FileOutputStream onlyRequiredNodesWriter = new FileOutputStream(
	// onlyRequiredNodes);) {
	//
	// Set<Long> nodesOfHighways = new HashSet<>();
	// while (true) {
	// OsmLight.SimpleWay simpleWay = OsmLight.SimpleWay
	// .parseDelimitedFrom(waysReader);
	// if (simpleWay == null) {
	// break;
	// }
	// for (Long node : simpleWay.getNodeList()) {
	// nodesOfHighways.add(node);
	// nodesOfHighwayss++;
	// }
	// }
	//
	// Map<Short, List<OsmLight.SimpleNode>> allNodes = new HashMap<>();
	// for (int i = 0; i < 120 * 360; i++) {
	// allNodes.put((short) i, new ArrayList<>());
	// }
	// while (true) {
	// OsmLight.SimpleNode simplenode = OsmLight.SimpleNode
	// .parseDelimitedFrom(nodesReader);
	// if (simplenode == null) {
	// break;
	// }
	// if (nodesOfHighways.contains(simplenode.getId())) {
	// uniqueNodesOfHighwayss++;
	// short encodedLonLat = ByteUtils.encodeLatLong(
	// simplenode.getLatBase(), simplenode.getLonBase());
	// allNodes.get(encodedLonLat).add(simplenode);
	// }
	// }
	//
	// Cartographer cartographer = new Cartographer();
	//
	// System.out.println("nodes of highways       : " + nodesOfHighwayss);
	// System.out.println("unique nodes of highways: "
	// + uniqueNodesOfHighwayss);
	//
	// }
	//
	// }

	// int[][] offsets = new int[360 * 120][];
	// byte[][] info = new byte[360 * 120][];
	// public static void main(String[] args) throws IOException,
	// URISyntaxException {

	// parseProtobuf(
	// new URL(
	// "http://download.geofabrik.de/europe/andorra-latest.osm.pbf")
	// .openStream(), OsmLight.SimpleWay.PARSER, way -> {
	// });

	// 12 -> 16 16 -> 012, 0
	// 2 byte prefix encodes lat, long (7 bit lat, 9 bit lon)
	// collect ALL data necessary for map. sort by id per segment.
	// translate id to offset inside. have 2^30 ids left.
	// make resulting array only long enough to fit amount of actual nodes.
	// eg 256 nodes -> 16 bit lat/lon + 8 bit/length.
	// point to offset instead of ID
	//
	// access 5.56,76.54 (5670)
	// data[576][offset[576][0]] ... data[576][offset[576][1 || max]] -1
	//
	// String url = "http://download.geofabrik.de/europe/" + name +
	// ".osm.pbf";

	// openStream = new URL(url).openStream();

	// }

	public static <T> void parseProtobuf(InputStream in, Parser<T> parser,
			Consumer<T> action) throws IOException {
		T parsed;
		while ((parsed = parser.parseDelimitedFrom(in)) != null) {
			action.accept(parsed);
		}
		in.close();
	}
}
