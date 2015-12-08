package osmlab;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
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
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.openstreetmap.osmosis.core.task.v0_6.RunnableSource;

import osmlab.io.Cartographer;
import osmlab.io.HighwayNodeSegmenter;
import osmlab.io.HighwayNodeSink;
import osmlab.proto.OsmLight.Neighbour;
import osmlab.proto.OsmLight.NewIdNode;
import osmlab.proto.OsmLight.OffsetData;
import osmlab.proto.OsmLight.PairConnection;
import osmlab.proto.OsmLight.SimpleNode;
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
		 {
		 LongOpenHashSet highwayNodes = new LongOpenHashSet();
		
		 // set of all highwaynodes and filtering and saving all ways in own
		 // format
		 cacheHighwayNodesAndWriteHighwaysToDisk(waysFile, highwayNodes,
		 new FileInputStream(inputFile));
		
		 // reading all nodes, filtering those who are in the set of highway
		 // nodes, and saving them in segmented format.
		 segmentAndSaveAllHighwayNodesWithCoordinates(fileName,
		 highwayNodes, inputFile);
		
		 // sort the nodes by id in each segment file
		 SimpleNode[][] segmentedSortedNodes = sortSegmentFiles(fileName);
		
		 createPairwiseWays(segmentedSortedNodes, waysFile, fileName);
		 }
		 createOffsetData(fileName);

		condenseOffsetDataAndCreateOffsetArray(fileName);

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
					ObjectOutputStream offsetOut = new ObjectOutputStream(new FileOutputStream(
							f.getAbsolutePath() + ".offset"));
					ObjectOutputStream dataOut = new ObjectOutputStream(new FileOutputStream(
							f.getAbsolutePath() + ".final"))) {
				while (dos.available() > 0) {
					OffsetData data = OffsetData.parseDelimitedFrom(dos);
					byte[] encoded = Cartographer.encodeOffsetData(data);
					dataBuffer.put(encoded);
					offsets.add(offsets.get(offsets.size()-1)+ encoded.length);
				}
				
				int[] offsetArray = new int[offsets.size()];
				for(int i = 0; i < offsetArray.length ; i++) {
					offsetArray[i] = offsets.get(i);
				}
				byte[] dataArray = new byte[dataBuffer.position()];
				dataBuffer.flip();

				dataBuffer.get(dataArray, 0, dataArray.length);
				
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

		List<PairConnection> pairs = new ArrayList<>();

		for (File f : files) {
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
						if (id < 100) {
							System.out.print(id + " : ");
							for (Neighbour n : data.getNeighbourList()) {
								System.out.print(n.getIdOfNode() + ", ");
							}
							System.out.println();
						}
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
			// // TODO save last one
			//
			// int[] offsetArray = new int[maxId + 2]; // TODO WTF?
			// System.out.println("maxId: " + maxId);
			//
			// // count number of neighbours for each index, temporarily store
			// in
			// // offset array
			// try (DataInputStream dos = new DataInputStream(
			// new ByteArrayInputStream(readAllBytes))) {
			// while (dos.available() > 0) {
			//
			// if (id + 1 < offsetArray.length) {
			// offsetArray[id + 1] += crossSegment ? 7 : 4;
			//
			// }
			// }
			// }
			// // offsets
			//
			// for (int i = 1; i < offsetArray.length; i++) {
			// offsetArray[i] += offsetArray[i - 1];
			//
			// }
			// for (int i = 1; i < offsetArray.length; i++) {
			// offsetArray[i] += i * 4;
			// }
			//
			// System.out.println(offsetArray[0]);
			// System.out.println(offsetArray[offsetArray.length - 2]);
			// System.out.println(offsetArray[offsetArray.length - 1]);
			//
			// int[] neighbourIndex = new int[offsetArray.length];
			// Arrays.fill(neighbourIndex, 4);
			//
			// int bytes = offsetArray[offsetArray.length - 1];
			// System.out.println("dataArraySize=" + bytes);
			// ByteBuffer dataArray = ByteBuffer.allocate(bytes);
			//
			// try (DataInputStream dos = new DataInputStream(
			// new ByteArrayInputStream(readAllBytes))) {
			// while (dos.available() > 0) {
			// int id = Byte.toUnsignedInt(dos.readByte()) << 16
			// | Short.toUnsignedInt(dos.readShort());
			// dataArray.position(offsetArray[id]);
			//
			// // always overwrite lat / lon offset
			// dataArray.putShort(dos.readShort()); // lat offset
			// dataArray.putShort(dos.readShort()); // lon offset
			//
			// // move pointer to current neighbour position(some
			// // neighbours may be present already)
			// dataArray.position(offsetArray[id] + neighbourIndex[id]);
			//
			// // copy flags (pedestrian, automobile, speed, crossSegment)
			// byte flags = dos.readByte();
			// boolean crossSegment = (Byte.toUnsignedInt(flags) & 1) > 0;
			// dataArray.put(flags);
			//
			// // copy over id this connection leads to
			// int idConnectedTo = Byte.toUnsignedInt(dos.readByte()) << 16
			// | Short.toUnsignedInt(dos.readShort());
			// if (id < 100) {
			// System.out.println(id + " -> " + idConnectedTo);
			// }
			// dataArray.put((byte) (idConnectedTo >>> 16));
			// dataArray.putShort((short) idConnectedTo);
			//
			// // if crossSegment, copy over lat/lon of that section
			// if (crossSegment) {
			// dataArray.put(dos.readByte());
			// dataArray.putShort(dos.readShort());
			//
			// }
			// neighbourIndex[id] += crossSegment ? 7 : 4;
			// }
			// }
			//
			// // for(int i = 0; i < 100; i++) {
			// // System.out.println(offsetArray[i]);
			// // }
			// dataArray.rewind();
			// // ByteBuffer bb = ByteBuffer.allocateDirect(offsetArray.length
			// // * Integer.BYTES);
			// // bb.order(ByteOrder.nativeOrder()); // endian must be set
			// before
			// // // putting ints into the buffer
			// // IntBuffer intB = bb.asIntBuffer();
			// // intB.put(offsetArray);
			// // System.out.println(offsetArray[0]);
			//
			// try (ObjectOutputStream offsetOut = new ObjectOutputStream(
			// new FileOutputStream(offsetFile));
			// ObjectOutputStream dataOut = new ObjectOutputStream(
			// new FileOutputStream(dataFile));) {
			// offsetOut.writeObject(offsetArray);
			// dataOut.writeObject(dataArray.array());
			// }
			// // try (FileOutputStream offsetStream = new FileOutputStream(
			// // offsetFile);
			// // FileOutputStream dataStream = new FileOutputStream(dataFile))
			// {
			// //
			// // while(bb.hasRemaining())offsetStream.getChannel().write(bb);
			// //
			// while(dataArray.hasRemaining())dataStream.getChannel().write(dataArray);
			// // }

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

				Neighbour.Builder toCurrent = Neighbour.newBuilder()
						.setCar(true).setPedestrian(true).setSpeed(16)
						.setIdOfNode(curNode.getNewId());

				Neighbour.Builder toPrev = Neighbour.newBuilder().setCar(true)
						.setPedestrian(true).setSpeed(16)
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
		// try N,E,S,W
		// int lat = ByteUtils.decodeLat(mostLikelyLatLon);
		// int lon = ByteUtils.decodeLon(mostLikelyLatLon);

		// int[] latLons = new int[4];
		// latLons[0] = ByteUtils.encodeLatLong(lat + 1, lon);
		// latLons[1] = ByteUtils.encodeLatLong(lat, lon + 1);
		// latLons[2] = ByteUtils.encodeLatLong(lat - 1, lon);
		// latLons[3] = ByteUtils.encodeLatLong(lat, lon - 1);
		//
		// for (int i = 0; i < 4; i++) {
		// if (latLons[i] >= 0 && latLons[i] <= Main.SEGMENTS) {
		// if (segmentedSortedNodes[latLons[i]] != null) {
		// NewIdNode nodePos = binarySearch(id,
		// segmentedSortedNodes[latLons[i]], latLons[i]);
		// if (nodePos != null) {
		// System.out.println("found in neighbours!");
		// return nodePos;
		//
		// }
		//
		// }
		// }
		// }

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

	// dos.readInt(); // skip lat/lon offset
	// /**
	// * connectionEncoding
	// *
	// * 1 pedestrian 1 automobile 5 speed 1 cross-segment 24 ID
	// * 24 latLon (optional, if cross-segment)
	// */
	// boolean crossSegment = (Byte.toUnsignedInt(dos.readByte()) & 1) > 0;
	// if (crossSegment) {
	// dos.readShort(); // skip latLon
	// dos.readByte(); // skip latLon
	// }
	// dos.readShort(); // skip ID 2/3
	// dos.readByte(); // skip ID 3/3
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
