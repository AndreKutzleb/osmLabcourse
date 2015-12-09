package osm.routing;
import it.unimi.dsi.fastutil.longs.LongArrayList;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import net.sf.geographiclib.Geodesic;
import net.sf.geographiclib.GeodesicData;
import osm.routing.MapDriver.MapSegment;
import osm.routing.MapDriver.SegmentNode;
import osmlab.sink.ByteUtils;

public class MapDatabase {

	// private final ByteBuffer[] data = new ByteBuffer[Main.SEGMENTS];
	// private final ByteBuffer[] offset = new ByteBuffer[Main.SEGMENTS];
	//
	// private int currentLonLat = -1;

	public static void main(String[] args) throws IOException,
			ClassNotFoundException, InterruptedException {

		MapDriver mapDriver = new MapDriver("germany-latest", true);

		// Thread.currentThread().sleep(1000000L);

		// System.out.println(mapDriver.get(12));

		for (int i = 0; i < 100; i++) {

			Random rand = new Random(i);

			SegmentNode fromNode = mapDriver.getRandomNode(rand);
			SegmentNode toNode = mapDriver.getRandomNode(rand);

			long to = ByteUtils.concat(toNode.seg.latLonBase, toNode.index);

			int toLatOffset = Byte
					.toUnsignedInt(toNode.seg.data[toNode.seg.offset[toNode.index]]) << 8
					| Byte.toUnsignedInt(toNode.seg.data[toNode.seg.offset[toNode.index] + 1]);

			int toLonOffset = Byte
					.toUnsignedInt(toNode.seg.data[toNode.seg.offset[toNode.index] + 2]) << 8
					| Byte.toUnsignedInt(toNode.seg.data[toNode.seg.offset[toNode.index] + 3]);

			double toLat = ByteUtils.reassembleDouble(
					ByteUtils.decodeLat(toNode.seg.latLonBase), toLatOffset) - 90;
			double toLon = ByteUtils.reassembleDouble(
					ByteUtils.decodeLon(toNode.seg.latLonBase), toLonOffset) - 180;

			long from = ByteUtils.concat(fromNode.seg.latLonBase,
					fromNode.index);
			int fromLatOffset = Byte
					.toUnsignedInt(fromNode.seg.data[fromNode.seg.offset[fromNode.index]]) << 8
					| Byte.toUnsignedInt(fromNode.seg.data[fromNode.seg.offset[fromNode.index] + 1]);

			int fromLonOffset = Byte
					.toUnsignedInt(fromNode.seg.data[fromNode.seg.offset[fromNode.index] + 2]) << 8
					| Byte.toUnsignedInt(fromNode.seg.data[fromNode.seg.offset[fromNode.index] + 3]);

			double fromLat = ByteUtils
					.reassembleDouble(
							ByteUtils.decodeLat(fromNode.seg.latLonBase),
							fromLatOffset) - 90;
			double fromLon = ByteUtils
					.reassembleDouble(
							ByteUtils.decodeLon(fromNode.seg.latLonBase),
							fromLonOffset) - 180;

			System.out.println(String.format(
					"calculating route from %s,%s to %s,%s", fromLat, fromLon,
					toLat, toLon));

			long beforeQueue = System.currentTimeMillis();
			{
				LongArrayList path = new LongArrayList();
				// LongOpenHash traversedFromTo = new LongOpenHashSet();
				Map<Long, Set<Long>> traversedFromTo = new HashMap<>();
				// first latLonBae, then id encoded, 32 bit each
				long current = from;

				while (true) {
					if (current == -1) {
						System.out.println("no way found!");
						break;
					}
					int currentLatLonBase = (int) (current >> 32);
					int currentId = (int) current;
					MapSegment currentSeg = mapDriver.get(currentLatLonBase);

					path.add(current);

					if (current == to) {
						System.out.println("found path of length "
								+ path.size() + "!");
						break;
					}

					int index = currentSeg.offset[currentId] + 4;
					int max = currentId + 1 == currentSeg.offset.length
							? currentSeg.data.length
							: currentSeg.offset[currentId + 1];

					int smallestRemainingDistance = Integer.MAX_VALUE;
					long nextBest = -1;

					while (index < max) {
						byte mask = currentSeg.data[index++];
						boolean crossesSegment = (mask & 1) > 0;

						int targetId = 0;
						targetId |= Byte
								.toUnsignedInt(currentSeg.data[index++]) << 16;
						targetId |= Byte
								.toUnsignedInt(currentSeg.data[index++]) << 8;
						targetId |= Byte
								.toUnsignedInt(currentSeg.data[index++]);

						// skip weight;
						index += 2;
						MapSegment targetSeg;
						if (crossesSegment) {
							int targetLatLonBase = 0;
							targetLatLonBase |= Byte
									.toUnsignedInt(currentSeg.data[index++]) << 16;
							targetLatLonBase |= Byte
									.toUnsignedInt(currentSeg.data[index++]) << 8;
							targetLatLonBase |= Byte
									.toUnsignedInt(currentSeg.data[index++]);
							targetSeg = mapDriver.get(targetLatLonBase);
						} else {
							targetSeg = currentSeg;
						}

						long target = ByteUtils.concat(targetSeg.latLonBase,
								targetId);

						int targetLatOffset = Byte
								.toUnsignedInt(targetSeg.data[targetSeg.offset[targetId]]) << 8
								| Byte.toUnsignedInt(targetSeg.data[targetSeg.offset[targetId] + 1]);
						int targetLonOffset = Byte
								.toUnsignedInt(targetSeg.data[targetSeg.offset[targetId] + 2]) << 8
								| Byte.toUnsignedInt(targetSeg.data[targetSeg.offset[targetId] + 3]);

						double targetLat = ByteUtils.reassembleDouble(
								ByteUtils.decodeLat(targetSeg.latLonBase),
								targetLatOffset) - 90;
						double targetLon = ByteUtils.reassembleDouble(
								ByteUtils.decodeLon(targetSeg.latLonBase),
								targetLonOffset) - 180;

						GeodesicData g = Geodesic.WGS84.Inverse(toLat, toLon,
								targetLat, targetLon);
						int distance = Math.max(1, (int) Math.round(g.s12));

						Set<Long> set = traversedFromTo.get(current);
						if ((set == null || !set.contains(target))
								&& distance < smallestRemainingDistance) {
							smallestRemainingDistance = distance;
							nextBest = target;
						}

					}
					Set<Long> set = traversedFromTo.get(current);
					if (set == null) {
						set = new HashSet<>();
						traversedFromTo.put(current, set);
					}
					set.add(nextBest);
					current = nextBest;
				}
			}

			long afterQueue = System.currentTimeMillis();		
			System.out.println("Time taken: " + (afterQueue - beforeQueue));

		}
	}

}
