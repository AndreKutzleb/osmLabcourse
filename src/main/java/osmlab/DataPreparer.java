package osmlab;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import net.sf.geographiclib.Geodesic;
import net.sf.geographiclib.GeodesicData;

import org.openstreetmap.osmosis.core.task.v0_6.RunnableSource;

import osmlab.io.Cartographer;
import osmlab.io.HighwayNodeSegmenter;
import osmlab.io.StatisticsSink;
import osmlab.proto.OsmLight.Neighbour;
import osmlab.proto.OsmLight.NewIdNode;
import osmlab.proto.OsmLight.OffsetData;
import osmlab.proto.OsmLight.PairConnection;
import osmlab.proto.OsmLight.SimpleNode;
import osmlab.proto.OsmLight.SimpleWay;
import osmlab.sink.ByteUtils;

import com.google.protobuf.Parser;

public class DataPreparer {

	public void prepareData(File inputFile) throws IOException {

		String fileName = inputFile.getName().substring(0,
				inputFile.getName().indexOf('.'));
		new File(fileName).mkdir();
		String waysFile = fileName + File.separator + fileName + ".ways.pbf";
		
		statisticsScan(waysFile, new FileInputStream(inputFile));
//		{
//			LongOpenHashSet highwayNodes = new LongOpenHashSet();
//
//			// set of all highwaynodes and filtering and saving all ways in own
//			// format
//			System.out.println("Extracting highways and caching ids");
//			cacheHighwayNodesAndWriteHighwaysToDisk(waysFile, highwayNodes,
//					new FileInputStream(inputFile));
//
//			System.out.println("splitting ways into segment files");
//			// reading all nodes, filtering those who are in the set of highway
//			// nodes, and saving them in segmented format.
//			segmentAndSaveAllHighwayNodesWithCoordinates(fileName,
//					highwayNodes, inputFile);
//
//			System.out.println("create sorted arrays of segmented nodes");
//			// sort the nodes by id in each segment file
//			SimpleNode[][] segmentedSortedNodes = sortSegmentFiles(fileName);
//
//			System.out.println("create pairwise ways");
//			createPairwiseWays(segmentedSortedNodes, waysFile, fileName);
//		}
//		createOffsetData(fileName);

	//	condenseOffsetDataAndCreateOffsetArray(fileName);

	}
	
	private void statisticsScan(String waysFile,InputStream inputStream)
			throws IOException {
		
		StatisticsSink sink = new StatisticsSink();
			
			RunnableSource reader;

			reader = new crosby.binary.osmosis.OsmosisReader(inputStream);

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

	
	
	private void condenseOffsetDataAndCreateOffsetArray(String fileName)
			throws IOException {
		File dataFolder = new File(fileName + File.separator + "data");

		File[] files = dataFolder.listFiles(file -> file.getName().endsWith(
				".data"));

		for (File f : files) {

			byte[] readAllBytes = Files.readAllBytes(f.toPath());

			ByteBuffer dataBuffer = ByteBuffer.allocate(readAllBytes.length);
			dataBuffer.order(ByteOrder.nativeOrder());
			ArrayList<Integer> offsets = new ArrayList<>();

			offsets.add(0);
			try (DataInputStream dos = new DataInputStream(
					new ByteArrayInputStream(readAllBytes));
					ObjectOutputStream offsetOut = new ObjectOutputStream(
							new FileOutputStream(f.getAbsolutePath()
									+ ".offset"));
					ObjectOutputStream dataOut = new ObjectOutputStream(
							new FileOutputStream(f.getAbsolutePath() + ".final"))) {
				while (dos.available() > 0) {
					OffsetData data = OffsetData.parseDelimitedFrom(dos);
					byte[] encoded = Cartographer.encodeOffsetData(data);
					dataBuffer.put(encoded);
					offsets.add(offsets.get(offsets.size() - 1)
							+ encoded.length);
				}

				int[] offsetArray = new int[offsets.size()];
				for (int i = 0; i < offsetArray.length; i++) {
					offsetArray[i] = offsets.get(i);
				}
				byte[] dataArray = new byte[dataBuffer.position()];
				dataBuffer.flip();

				dataBuffer.get(dataArray, 0, dataArray.length);

				f.delete();
				offsetOut.writeObject(offsetArray);
				dataOut.writeObject(dataArray);

			}

		}

	}
	private void createOffsetData(String fileName)
			throws FileNotFoundException, IOException {
		File pairFolder = new File(fileName + File.separator
				+ "pairwiseConnections");

		File dataFolder = new File(fileName + File.separator + "data");
		dataFolder.mkdir();

		File[] files = pairFolder.listFiles(file -> file.getName().endsWith(
				".pairs"));


		int handled = 0;
		for (File f : files) {
			List<PairConnection> pairs = new ArrayList<>();
			byte[] readAllBytes = Files.readAllBytes(f.toPath());

			try (DataInputStream dos = new DataInputStream(
					new ByteArrayInputStream(readAllBytes))) {
				while (dos.available() > 0) {
					PairConnection pair = PairConnection
							.parseDelimitedFrom(dos);
					pairs.add(pair);
				}
			}

			pairs.sort((a, b) -> Integer.compare(a.getId(), b.getId()));
			// pairs.stream().collect(collector)

			PairConnection previous = pairs.get(0);
			OffsetData.Builder builder = OffsetData.newBuilder()
					.setLatOffset(previous.getLatOffset())
					.setLonOffset(previous.getLonOffset())
					.addNeighbour(previous.getNeighbour());

			File dataFile = new File(dataFolder.getAbsolutePath()
					+ File.separator
					+ f.getName().substring(0, f.getName().indexOf('.'))
					+ ".data");

			try (FileOutputStream dataOut = new FileOutputStream(dataFile);) {

				int id = 0;
				for (int index = 1; index < pairs.size(); index++) {
					PairConnection current = pairs.get(index);
					if (previous.getId() != current.getId()) {
						// wrap up previous
						OffsetData data = builder.build();
						data.writeDelimitedTo(dataOut);
						id++;
						builder = OffsetData.newBuilder()
								.setLatOffset(current.getLatOffset())
								.setLonOffset(current.getLonOffset())
								.addNeighbour(current.getNeighbour());

					} else {
						builder.addNeighbour(current.getNeighbour());
					}
					previous = current;
				}
				builder.build().writeDelimitedTo(dataOut);
			}
			
			System.out.println("File "+ ++handled + "/" + files.length + " handled.");

		}
	}
	private void createPairwiseWays(SimpleNode[][] segmentedSortedNodes,
			String waysFile, String mapName) throws IOException {
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

	private void handleWay(SimpleWay way, SimpleNode[][] segmentedSortedNodes,
			Cartographer cartographer) {
		if (way.getNodeCount() > 1) {
			NewIdNode prevNode = findNodeData(way.getNode(0),
					segmentedSortedNodes, 0);
			for (int i = 1; i < way.getNodeCount(); i++) {
				long id = way.getNodeList().get(i);
				NewIdNode curNode = findNodeData(id, segmentedSortedNodes,
						prevNode.getLatLonBase());

				// TODO speed, pedestrian, car
				double prevLat = ByteUtils.reassembleDouble(
						ByteUtils.decodeLat(prevNode.getLatLonBase()),
						prevNode.getLatOffset()) - 90;
				double prevLon = ByteUtils.reassembleDouble(
						ByteUtils.decodeLon(prevNode.getLatLonBase()),
						prevNode.getLonOffset()) - 180;

				double currLat = ByteUtils.reassembleDouble(
						ByteUtils.decodeLat(curNode.getLatLonBase()),
						curNode.getLatOffset()) - 90;
				double currLon = ByteUtils.reassembleDouble(
						ByteUtils.decodeLon(curNode.getLatLonBase()),
						curNode.getLonOffset()) - 180;

				GeodesicData g = Geodesic.WGS84.Inverse(prevLat, prevLon,
						currLat, currLon);
				int distance = Math.max(1, (int) Math.round(g.s12));
				if (distance > Math.pow(2, 16)) {
					String error = String.format("distance between %s,%s and %s,%s too large (%s > 2^16)",prevLat,prevLon,currLat,currLon, distance);
					throw new IllegalArgumentException(error);
				}

				Neighbour.Builder toCurrent = Neighbour.newBuilder()
						.setCar(true).setPedestrian(true).setSpeed(16)
						.setDistance(distance).setIdOfNode(curNode.getNewId());

				Neighbour.Builder toPrev = Neighbour.newBuilder().setCar(true)
						.setPedestrian(true).setSpeed(16).setDistance(distance)
						.setIdOfNode(prevNode.getNewId());

				if (prevNode.getLatLonBase() != curNode.getLatLonBase()) {
					toCurrent.setLatLonOfSegment(curNode.getLatLonBase());
					toPrev.setLatLonOfSegment(prevNode.getLatLonBase());

				}

				PairConnection toCurrentConnection = PairConnection
						.newBuilder().setId(prevNode.getNewId())
						.setLatOffset(prevNode.getLatOffset())
						.setLonOffset(prevNode.getLonOffset())
						.setNeighbour(toCurrent).build();

				PairConnection toPrevConnection = PairConnection.newBuilder()
						.setId(curNode.getNewId())
						.setLatOffset(curNode.getLatOffset())
						.setLonOffset(curNode.getLonOffset())
						.setNeighbour(toPrev).build();

				// if (toCurrentConnection.getId() < 100) {
				// // System.out.println(toCurrentConnection.getId() +" -> " +
				// // toCurrentConnection.getNeighbour().getIdOfNode());
				// // System.out.println(toPrevConnection.getId() +" -> " +
				// // toPrevConnection.getNeighbour().getIdOfNode());
				//
				// }

				try {
					cartographer.writePairwiseConnection(
							prevNode.getLatLonBase(), toCurrentConnection);
					cartographer.writePairwiseConnection(
							curNode.getLatLonBase(), toPrevConnection);
				} catch (IOException e) {
					e.printStackTrace();
				}

				prevNode = curNode;
			}
		}

	}

	// public static class NodePos {
	// public final int latLonBase;
	// public final int index;
	// public final int latOffset;
	// public final int lonOffset;
	//
	// public NodePos(int latOffset, int lonOffset, int latLonBase, int index) {
	// this.latOffset = latOffset;
	// this.lonOffset = lonOffset;
	// this.latLonBase = latLonBase;
	// this.index = index;
	// }
	//
	// public static NodePos parseFrom(SimpleNode[] nodes, int middle, int
	// latLonBase) {
	// int lat = (int) (nodes[middle].getLatOffset());
	// int lon = (int) nodes[middle].getLonOffset();
	// return new NodePos(lat, lon, latLonBase, middle);
	// }
	// }

	// search for 64 bit id, get index of id in segment, as well as lat and lon.
	private NewIdNode findNodeData(long id,
			SimpleNode[][] segmentedSortedNodes, int mostLikelyLatLon) {
		// try to find in same or neighbouring blocks before doing a full search
		SimpleNode[] mostLikely = segmentedSortedNodes[mostLikelyLatLon];

		if (mostLikely != null) {
			NewIdNode nodePos = binarySearch(id, mostLikely, mostLikelyLatLon);
			if (nodePos != null) {
				return nodePos;

			}
		}

		for (int i = 0; i < segmentedSortedNodes.length; i++) {
			SimpleNode[] nodes = segmentedSortedNodes[i];
			if (nodes != null) {
				NewIdNode nodePos = binarySearch(id, nodes, i);
				if (nodePos != null) {
					return nodePos;
				}
			}
		}

		throw new IllegalStateException("data for node could not be found");
	}

	// 10-0

	public NewIdNode binarySearch(long id, SimpleNode[] nodes, int latLonBase) {

		if (id > nodes[nodes.length - 1].getId() || id < nodes[0].getId()) {
			return null;
		}

		int low = 0;
		int high = nodes.length - 1;

		while (high >= low) {
			int middle = (low + high) / 2;
			if (nodes[middle].getId() == id) {
				return NewIdNode.newBuilder().setLatLonBase(latLonBase)
						.setLatOffset(nodes[middle].getLatOffset())
						.setLonOffset(nodes[middle].getLonOffset())
						.setNewId(middle).build();
			}
			if (nodes[middle].getId() < id) {
				low = middle + 1;
			}
			if (nodes[middle].getId() > id) {
				high = middle - 1;
			}
		}
		return null;
	}

	private SimpleNode[][] sortSegmentFiles(String fileName)
			throws FileNotFoundException, IOException {
		String highwayNodesFolderName = fileName + File.separator
				+ "highwayNodes";

		SimpleNode[][] segmentedSortedNodeIds = new SimpleNode[Main.SEGMENTS][];

		File[] files = new File(highwayNodesFolderName).listFiles(f -> f
				.getName().endsWith(".nodes"));
		int filesProcessed = 0;
		for (File f : files) {
			String rawFileName = f.getName().substring(0,
					f.getName().indexOf('.'));
			int latLon = Integer.parseInt(rawFileName);

			ArrayList<SimpleNode> nodes = new ArrayList<>();
			try (DataInputStream dis = new DataInputStream(
					new BufferedInputStream(new FileInputStream(f)));) {

				while (dis.available() > 0) {
					SimpleNode node = SimpleNode.parseDelimitedFrom(dis);
					nodes.add(node);
				}
			}
			Collections.sort(nodes,
					(a, b) -> Long.compareUnsigned(a.getId(), b.getId()));
			SimpleNode[] sortedIds = nodes.toArray(new SimpleNode[0]);

			segmentedSortedNodeIds[latLon] = sortedIds;

			filesProcessed++;
			System.out.println(filesProcessed + " nodes Processed");

		}
		return segmentedSortedNodeIds;
	}

	private void cacheHighwayNodesAndWriteHighwaysToDisk(String waysFile,
			LongOpenHashSet highwayNodes, InputStream inputStream)
			throws IOException {
//		try (FileOutputStream waysWriter = new FileOutputStream(new File(
//				waysFile));) {
//
////			AbstractHighwaySink hNodeSink = new AbstractHighwaySink(waysWriter,
////					highwayNodes);
//
//			RunnableSource reader;
//
//			reader = new crosby.binary.osmosis.OsmosisReader(inputStream);
//
//			reader.setSink(hNodeSink);
//
//			Thread readerThread = new Thread(reader);
//			readerThread.start();
//
//			while (readerThread.isAlive()) {
//				try {
//					readerThread.join();
//				} catch (InterruptedException e) {
//					e.printStackTrace();
//				}
//			}
//
//		}
	}

	private void segmentAndSaveAllHighwayNodesWithCoordinates(String mapName,
			LongOpenHashSet highwayNodes, File inputFile) throws IOException {

		try (Cartographer cartographer = new Cartographer(mapName)) {

//			HighwayNodeSegmenter hNodeSink = new HighwayNodeSegmenter(
//					cartographer, highwayNodes);

			RunnableSource reader;

			reader = new crosby.binary.osmosis.OsmosisReader(
					new FileInputStream(inputFile));

//			reader.setSink(hNodeSink);

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

	public static <T> void parseProtobuf(InputStream in, Parser<T> parser,
			Consumer<T> action) throws IOException {
		T parsed;
		while ((parsed = parser.parseDelimitedFrom(in)) != null) {
			action.accept(parsed);
		}
		in.close();
	}
}
