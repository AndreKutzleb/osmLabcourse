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

import javax.sql.rowset.CachedRowSet;

import org.junit.experimental.categories.Categories;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;

import osm.preprocessing.DataProcessor;
import osm.preprocessing.PipelineParts.PipelinePaths;
import osmlab.io.AbstractHighwaySink;
import osmlab.io.SimpleSink;
import osmlab.sink.ByteUtils;
import osmlab.sink.FormatConstants;
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
				InputStream is = new FileInputStream(paths.SOURCE_FILE);
				ObjectOutputStream dataArrayStream = new ObjectOutputStream(
						new BufferedOutputStream(new FileOutputStream(
								paths.DATA_ARRAY)));
				ObjectOutputStream offsetArrayStream = new ObjectOutputStream(
						new BufferedOutputStream(new FileOutputStream(
								paths.OFFSET_ARRAY)));

		) {

			int nodeCount = (int) highwayNodesSortedSizes.readInt();
			int dataSize = dataArraySize.readInt();

			long[] allNodes = new long[nodeCount];
			int[] offsetArray = new int[nodeCount];
			int[] data = new int[dataSize];

			// read raw unsorted node ids with duplicates
			for (int i = 0; i < nodeCount; i++) {
				allNodes[i] = highwayNodesSorted.readLong();
				offsetArray[i] = offsetArrayRaw.readInt();

				if (i % (nodeCount / 100) == 0) {
					progressHandler.accept("Reading raw Node IDs and offsets",
							i, nodeCount);
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
				public void handleHighway(Way way) {
					highways++;

					if ((highways + nodes) % (expectedHighwaysAndNodes / 100) == 0) {
						progressHandler
								.accept("Filling coordinates and link information into data array",
										(int) (highways + nodes),
										expectedHighwaysAndNodes);
					}

					// remember one link for each direction
					for (int i = 1; i < way.getWayNodes().size(); i++) {

						long firstNode = way.getWayNodes().get(i - 1)
								.getNodeId();
						long secondNode = way.getWayNodes().get(i).getNodeId();

						int indexOfFirstNode = Arrays.binarySearch(allNodes,
								firstNode);
						int indexOfSecondNode = Arrays.binarySearch(allNodes,
								secondNode);

						addEdgeFromTo(indexOfFirstNode, indexOfSecondNode);
						addEdgeFromTo(indexOfSecondNode, indexOfFirstNode);

					}
				}

				private void addEdgeFromTo(int indexOfFirstNode,
						int indexOfSecondNode) {
					int offset = offsetArray[indexOfFirstNode];
					offset += FormatConstants.CONSTANT_NODESIZE; // Skip LAT /
																	// LON. We
																	// want to
																	// enter the
																	// edge only
					// There may be neighbours already, skip to first 0-spot
					// (assuming no neighbour yet defaults to 0 in dataArray)
					while (data[offset] != 0) {
						offset++;
					}
					// fill in data of connection - target id, speed and
					// pedestrian yes/no
					int edge = ByteUtils.encodeEdge(indexOfSecondNode, true,
							(byte) 15);
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
					// Write our final datastructures to disk as serialized java
					// arrays - offsetarray and data array.
					try {
						progressHandler.accept("Writing data array to disc",
								-1, -1);
						dataArrayStream.writeObject(data);
						progressHandler.accept("Writing offset array to disc",
								-1, -1);
						offsetArrayStream.writeObject(offsetArray);
					} catch (IOException e) {
						e.printStackTrace();
					}

				}
			};

			OsmUtils.readFromOsm(is, s);
		}
	}

	// Original highway parse code by Jonas Grunert

	public static HighwayInfos extractHighwayInfos(Way way) {
		String highway = null;
		String maxspeed = null;
		String sidewalk = null;
		String oneway = null;
		String access = null;

		for (Tag tag : way.getTags()) {
			switch (tag.getKey()) {
				case "highway" :
					highway = tag.getValue();
					break;
				case "maxspeed" :
					maxspeed = tag.getValue();
					break;
				case "sidewalk" :
					sidewalk = tag.getValue();
					break;
				case "oneway" :
					oneway = tag.getValue();
					break;
				case "access" :
					access = tag.getValue();
					break;
				default :
					break;
			}
		}

		boolean accessOk;
		if (access != null
				&& (access.equals("private") || access.equals("no")
						|| access.equals("emergency")
						|| access.equals("agricultural") || access
							.equals("bus"))) {
			accessOk = false;
		} else {
			accessOk = true;
		}
		HighwayInfos hw = null;
		try {
			if (highway != null && accessOk) {
				hw = evaluateHighway(highway, maxspeed, sidewalk, oneway);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return hw;
	}

	private int speedBitsToKmh(int speedBits) {

		switch (speedBits) {
		// pedestrian only
			case 0 :
				return 0;
				// walking speed
			case 1 :
				return 5;
				// unlimited
			case 15 :
				return 300;
				// 2-14 -> 10 - 130 kmh (10kmh steps)
			default :
				return (speedBits - 1) * 10;
		}

	}

	final static int[] categories = new int[]{0, 5, 10, 20, 30, 40, 50, 60, 70,
			80, 90, 100, 110, 120, 130, 250};

	private static Byte parseSpeed(String maxspeedTag, boolean mph) {
		int maxSpeed = Integer.parseInt(maxspeedTag);

		if (mph) {
			maxSpeed = Math.round(maxSpeed * 1.60934f);
		}

		byte speedBits = -1;
		for (int i = 1; i < categories.length; i++) {
			if (Math.abs(categories[i - 1] - maxSpeed) < (Math
					.abs(categories[i] - maxSpeed))) {
				speedBits = (byte) (i - 1);
				break;
			}
		}
		// in case the speedbits are still 0, the value is closer to
		// unlimited(250) than 130. in this case, use
		// unlimited
		if (speedBits == -1) {
			speedBits = 15;
		}

		return speedBits;
	}
	// Code by Jonas Grunert
	private static HighwayInfos evaluateHighway(String highwayTag,
			String maxspeedTag, String sidewalkTag, String onewayTag) {

		// Try to find out maxspeed
		Byte maxSpeed;
		if (maxspeedTag != null) {
			if (maxspeedTag.equals("none") || maxspeedTag.equals("signals")
					|| maxspeedTag.equals("variable")
					|| maxspeedTag.equals("unlimited")) {
				// No speed limitation
				maxSpeed = 15;
			} else if (maxspeedTag.contains("living_street")) {
				maxSpeed = 1;
			} else if (maxspeedTag.contains("walk")
					|| maxspeedTag.contains("foot")) {
				maxSpeed = 0;
			} else {
				try {
					boolean mph = false;
					// Try to parse speed limit
					if (maxspeedTag.contains("mph")) {
						mph = true;
						maxspeedTag = maxspeedTag.replace("mph", "");
					}
					if (maxspeedTag.contains("km/h"))
						maxspeedTag = maxspeedTag.replace("km/h", "");
					if (maxspeedTag.contains("kmh"))
						maxspeedTag = maxspeedTag.replace("kmh", "");
					if (maxspeedTag.contains("."))
						maxspeedTag = maxspeedTag.split("\\.")[0];
					if (maxspeedTag.contains(","))
						maxspeedTag = maxspeedTag.split(",")[0];
					if (maxspeedTag.contains(";"))
						maxspeedTag = maxspeedTag.split(";")[0];
					if (maxspeedTag.contains("-"))
						maxspeedTag = maxspeedTag.split("-")[0];
					if (maxspeedTag.contains(" "))
						maxspeedTag = maxspeedTag.split(" ")[0];

					maxSpeed = parseSpeed(maxspeedTag, mph);

				} catch (Exception e) {
					e.printStackTrace();
					maxSpeed = null;
				}
			}
		} else {
			maxSpeed = null;
		}

		// Try to find out if has sidewalk
		SidewalkMode sidewalk = SidewalkMode.Unspecified;
		if (sidewalkTag != null) {
			if (sidewalkTag.equals("no") || sidewalkTag.equals("none")) {
				sidewalk = SidewalkMode.No;
			} else {
				sidewalk = SidewalkMode.Yes;
			}
		}

		// Try to find out if is oneway
		Boolean oneway;
		if (onewayTag != null) {
			oneway = onewayTag.equals("yes");
		} else {
			oneway = null;
		}

		// Try to classify highway
		if (highwayTag.equals("track")) {
			// track
			if (maxSpeed == null)
				maxSpeed = 2;
			if (oneway == null)
				oneway = false;

			return new HighwayInfos(true, true, oneway, maxSpeed);
		} else if (highwayTag.equals("residential")) {
			// residential road
			if (maxSpeed == null)
				maxSpeed = 6;
			if (oneway == null)
				oneway = false;

			// Residential by default with sideway
			return new HighwayInfos(true, (sidewalk != SidewalkMode.No),
					oneway, maxSpeed);
		} else if (highwayTag.equals("service")) {
			// service road
			if (maxSpeed == null)
				maxSpeed = 4;
			if (oneway == null)
				oneway = false;

			// Residential by default with sideway
			return new HighwayInfos(true, (sidewalk != SidewalkMode.No),
					oneway, maxSpeed);
		} else if (highwayTag.equals("footway") || highwayTag.equals("path")
				|| highwayTag.equals("steps") || highwayTag.equals("bridleway")
				|| highwayTag.equals("pedestrian")) {
			// footway etc.
			if (maxSpeed == null)
				maxSpeed = 0;
			if (oneway == null)
				oneway = false;

			return new HighwayInfos(false, true, oneway, maxSpeed);
		} else if (highwayTag.startsWith("primary")) {
			// country road etc
			if (maxSpeed == null)
				maxSpeed = 11;
			if (oneway == null)
				oneway = false;

			return new HighwayInfos(true, (sidewalk == SidewalkMode.Yes),
					oneway, maxSpeed);
		} else if (highwayTag.startsWith("secondary")
				|| highwayTag.startsWith("tertiary")) {
			// country road etc
			if (maxSpeed == null)
				maxSpeed = 11;
			if (oneway == null)
				oneway = false;

			return new HighwayInfos(true, (sidewalk != SidewalkMode.No),
					oneway, maxSpeed);
		} else if (highwayTag.equals("unclassified")) {
			// unclassified (small road)
			if (maxSpeed == null)
				maxSpeed = 6;
			if (oneway == null)
				oneway = false;

			return new HighwayInfos(true, (sidewalk != SidewalkMode.No),
					oneway, maxSpeed);
		} else if (highwayTag.equals("living_street")) {
			// living street
			if (maxSpeed == null)
				maxSpeed = 1;
			if (oneway == null)
				oneway = false;

			return new HighwayInfos(true, true, oneway, maxSpeed);
		} else if (highwayTag.startsWith("motorway")) {
			// track
			if (maxSpeed == null)
				maxSpeed = 15;
			if (oneway == null)
				oneway = true;

			return new HighwayInfos(true, (sidewalk == SidewalkMode.Yes),
					oneway, maxSpeed);
		} else if (highwayTag.startsWith("trunk")) {
			// trunk road
			if (maxSpeed == null)
				maxSpeed = 15;
			if (oneway == null)
				oneway = false;

			return new HighwayInfos(true, (sidewalk == SidewalkMode.Yes),
					oneway, maxSpeed);
		}

		// Ignore this road if no useful classification available
		return null;
	}

	enum SidewalkMode {
		Yes, No, Unspecified
	};

	private static class HighwayInfos {
		public final boolean Oneway;
		public final boolean pedestrian;
		public final byte MaxSpeed;

		public HighwayInfos(boolean car, boolean pedestrian, boolean oneway,
				byte maxSpeed) {

			this.pedestrian = pedestrian;
			this.Oneway = oneway;
			this.MaxSpeed = maxSpeed;
		}

	}

}
