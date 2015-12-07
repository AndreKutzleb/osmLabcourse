package osmlab;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.openstreetmap.osmosis.core.task.v0_6.RunnableSource;

import osmlab.io.Cartographer;
import osmlab.io.HighwayNodeSegmenter;
import osmlab.io.HighwayNodeSink;
import osmlab.proto.OsmLight;
import osmlab.sink.ByteUtils;

import com.carrotsearch.hppc.LongOpenHashSet;
import com.google.protobuf.Parser;

public class DataPreparer {

		public void prepareData(File inputFile) throws IOException {

			String fileName = inputFile.getName().substring(0, inputFile.getName().indexOf('.'));
			new File(fileName).mkdir();
			String waysFile = fileName + File.separator + fileName + ".ways.pbf";

			LongOpenHashSet highwayNodes = new LongOpenHashSet();

			// set of all highwaynodes and filtering and saving all ways in own format
			cacheHighwayNodesAndWriteHighwaysToDisk(waysFile,highwayNodes,new FileInputStream(inputFile));
			
			// reading all nodes, filtering those who are in the set of highway nodes, and saving them in segmented format.
			segmentAndSaveAllHighwayNodesWithCoordinates(fileName,highwayNodes, inputFile);
		
		}
		
		private void cacheHighwayNodesAndWriteHighwaysToDisk(String waysFile, LongOpenHashSet highwayNodes, InputStream inputStream) throws IOException {
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

				HighwayNodeSegmenter hNodeSink = new HighwayNodeSegmenter(cartographer,highwayNodes);

				RunnableSource reader;

				reader = new crosby.binary.osmosis.OsmosisReader(new FileInputStream(inputFile));

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

		
//	public static void merge(File ways, File nodes, File onlyRequiredNodes)
//				throws FileNotFoundException, IOException {
//			try (FileInputStream waysReader = new FileInputStream(ways);
//					FileInputStream nodesReader = new FileInputStream(nodes);
//					FileOutputStream onlyRequiredNodesWriter = new FileOutputStream(
//							onlyRequiredNodes);) {
//
//				Set<Long> nodesOfHighways = new HashSet<>();
//				while (true) {
//					OsmLight.SimpleWay simpleWay = OsmLight.SimpleWay
//							.parseDelimitedFrom(waysReader);
//					if (simpleWay == null) {
//						break;
//					}
//					for (Long node : simpleWay.getNodeList()) {
//						nodesOfHighways.add(node);
//						nodesOfHighwayss++;
//					}
//				}
//
//				Map<Short, List<OsmLight.SimpleNode>> allNodes = new HashMap<>();
//				for (int i = 0; i < 120 * 360; i++) {
//					allNodes.put((short) i, new ArrayList<>());
//				}
//				while (true) {
//					OsmLight.SimpleNode simplenode = OsmLight.SimpleNode
//							.parseDelimitedFrom(nodesReader);
//					if (simplenode == null) {
//						break;
//					}
//					if (nodesOfHighways.contains(simplenode.getId())) {
//						uniqueNodesOfHighwayss++;
//						short encodedLonLat = ByteUtils.encodeLatLong(
//								simplenode.getLatBase(), simplenode.getLonBase());
//						allNodes.get(encodedLonLat).add(simplenode);
//					}
//				}
//
//				Cartographer cartographer = new Cartographer();
//
//				System.out.println("nodes of highways       : " + nodesOfHighwayss);
//				System.out.println("unique nodes of highways: "
//						+ uniqueNodesOfHighwayss);
//
//			}
//
//		}

//		int[][] offsets = new int[360 * 120][];
//		byte[][] info = new byte[360 * 120][];
//		public static void main(String[] args) throws IOException,
//				URISyntaxException {

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


		}

//		public static <T> void parseProtobuf(InputStream in, Parser<T> parser,
//				Consumer<T> action) throws IOException {
//			T parsed;
//			while ((parsed = parser.parseDelimitedFrom(in)) != null) {
//				action.accept(parsed);
//			}
//			in.close();
//		}

	


