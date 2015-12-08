import it.unimi.dsi.fastutil.PriorityQueues;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntHeapPriorityQueue;
import it.unimi.dsi.fastutil.ints.IntPriorityQueues;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

import com.carrotsearch.hppc.LongSet;

import net.sf.geographiclib.Geodesic;
import net.sf.geographiclib.GeodesicData;
import osmlab.sink.ByteUtils;

public class MapDatabase {

	// private final ByteBuffer[] data = new ByteBuffer[Main.SEGMENTS];
	// private final ByteBuffer[] offset = new ByteBuffer[Main.SEGMENTS];
	//
	// private int currentLonLat = -1;

	public static void main(String[] args) throws IOException,
			ClassNotFoundException, InterruptedException {
		
		
		MapDriver mapDriver = new MapDriver("baden-wuerttemberg-latest",true);
		
		Thread.currentThread().sleep(1000000L);
		
		System.out.println(mapDriver.get(12));
//		
//		Random rand = new Random(1311);
//		int from = rand.nextInt(offsetBuffer.length);
//		int to = rand.nextInt(offsetBuffer.length);
//
//		int toLatOffset = Byte.toUnsignedInt(dataBuffer[offsetBuffer[to]]) << 8
//				| Byte.toUnsignedInt(dataBuffer[offsetBuffer[to] + 1]);
//		int toLonOffset = Byte.toUnsignedInt(dataBuffer[offsetBuffer[to] + 2]) << 8
//				| Byte.toUnsignedInt(dataBuffer[offsetBuffer[to] + 3]);
//
//		double toLat = ByteUtils.reassembleDouble(ByteUtils.decodeLat(latLon),
//				toLatOffset) - 90;
//		double toLon = ByteUtils.reassembleDouble(
//				ByteUtils.decodeLon(latLon), toLonOffset) - 180;
//
//		
//		long beforeQueue = System.currentTimeMillis();
//		{
//			IntArrayList path = new IntArrayList();
//			LongOpenHashSet traversedFromTo = new LongOpenHashSet();
//			
//			int nextToVisit = from;
//			while(true) {
//		
//				path.add(nextToVisit);
////				System.out.println(nextToVisit+ " ");
//				
//				if(nextToVisit == to) {
//					System.out.println("found target");
//					break;
//				}
//				
//
//				int index = offsetBuffer[nextToVisit] + 4;
//				int max = nextToVisit + 1 == offsetBuffer.length
//						? dataBuffer.length
//						: offsetBuffer[nextToVisit + 1];
//
//				int smallestRemainingDistance = Integer.MAX_VALUE;
//				int nextBest = -1;
//				
//				while (index < max) {
//					byte mask = dataBuffer[index++];
//					boolean crossesSegment = (mask & 1) > 0;
//
//					int target = 0;
//					target |= Byte.toUnsignedInt(dataBuffer[index++]) << 16;
//					target |= Byte.toUnsignedInt(dataBuffer[index++]) << 8;
//					target |= Byte.toUnsignedInt(dataBuffer[index++]);
//
//					// skip weight;
//					index+=2;
//					if (!crossesSegment) {
//						
//
//						int neighbourLatOffset = Byte.toUnsignedInt(dataBuffer[offsetBuffer[target]]) << 8
//								| Byte.toUnsignedInt(dataBuffer[offsetBuffer[target] + 1]);
//						int neighbourLonOffset = Byte.toUnsignedInt(dataBuffer[offsetBuffer[target] + 2]) << 8
//								| Byte.toUnsignedInt(dataBuffer[offsetBuffer[target] + 3]);
//
//						double neighbourLat = ByteUtils.reassembleDouble(ByteUtils.decodeLat(latLon),
//								neighbourLatOffset) - 90;
//						double neighbourLon = ByteUtils.reassembleDouble(
//								ByteUtils.decodeLon(latLon), neighbourLonOffset) - 180;
//
//						
//						GeodesicData g = Geodesic.WGS84.Inverse(toLat, toLon,
//								neighbourLat, neighbourLon);
//						int distance = Math.max(1,(int) Math.round(g.s12));
//						
//						if((!traversedFromTo.contains(Integer.toUnsignedLong(nextToVisit) << 32 | Integer.toUnsignedLong(target))) && distance < smallestRemainingDistance) {
//							smallestRemainingDistance = distance;
//							nextBest = target;
//						}
//	
//					}
//					if (crossesSegment) {
//						index += 3;
//					}
//
//				}
//				traversedFromTo.add(Integer.toUnsignedLong(nextToVisit) << 32 | Integer.toUnsignedLong(nextBest));
//				nextToVisit = nextBest;
//
//			}
//		}
//
//		long afterQueue = System.currentTimeMillis();

		// {
		// {
		// int[] visitedBy = new int[offsetBuffer.length];
		// Arrays.fill(visitedBy, -1);
		// int[] visitQueue = new int[offsetBuffer.length];
		// int nextToVisitIndex = 0;
		// int nextFreeIndex = 1;
		// visitQueue[0] = from;
		//
		// searchLoop: while (nextToVisitIndex < nextFreeIndex) {
		//
		// if(nextToVisitIndex == nextFreeIndex) {
		// System.out.println("not found");
		// break;
		// }
		// int toVisit = visitQueue[nextToVisitIndex++];
		//
		// int index = offsetBuffer[toVisit] + 4;
		// int max = toVisit + 1 == offsetBuffer.length
		// ? dataBuffer.length
		// : offsetBuffer[toVisit + 1];
		//
		// while (index < max) {
		// byte mask = dataBuffer[index++];
		// boolean crossesSegment = (mask & 1) > 0;
		//
		// int target = 0;
		// target |= Byte.toUnsignedInt(dataBuffer[index++]) << 16;
		// target |= Byte.toUnsignedInt(dataBuffer[index++]) << 8;
		// target |= Byte.toUnsignedInt(dataBuffer[index++]);
		//
		// if (!crossesSegment) {
		// if (visitedBy[target] == -1) {
		// visitedBy[target] = toVisit;
		// visitQueue[nextFreeIndex++] = target;
		// }
		// if (visitedBy[target] == to) {
		// System.out.println("target reached!");
		// break searchLoop;
		// }
		//
		// }
		// if (crossesSegment) {
		// index += 3;
		// // TODO
		// // dos.writeByte(latLon >>> 16);
		// // dos.writeByte(latLon >>> 8);
		// // dos.writeByte(latLon);
		// }
		//
		// }
		//
		// }
		// }
		// }
//		long afterArray = System.currentTimeMillis();
//
//		System.out.println("With queue: " + (afterQueue - beforeQueue));
//		System.out.println("With array: " + (afterArray - afterQueue));
//
//		// for(Integer from : neighbours.keySet()) {
//		// System.out.println(from + " -> " + neighbours);
//		// }
//		System.out.println(neighbours);

	}

}
