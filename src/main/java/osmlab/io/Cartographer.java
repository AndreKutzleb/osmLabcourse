package osmlab.io;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.openstreetmap.osmosis.core.domain.v0_6.Node;

import osmlab.proto.OsmLight.Neighbour;
import osmlab.proto.OsmLight.OffsetData;
import osmlab.proto.OsmLight.PairConnection;
import osmlab.proto.OsmLight.SimpleNode;
import osmlab.sink.ByteUtils;

public class Cartographer implements AutoCloseable {

	private final Map<Integer, DataOutputStream> dataWriters = new HashMap<>();

	private final Map<Integer, DataOutputStream> pairwiseConnectionWriters = new HashMap<>();

	private final File pairwiseConnections;
	private final File highwayNodesFile;


	// intermediate format:
	// long actualId
	// short lat
	// short lon

//	public static class SimpleNode implements Comparable<SimpleNode>{
//		public final long id;
//		public final short lat;
//		public final short lon;
//		
//		
//		public SimpleNode(long id, short lat, short lon) {
//			this.id = id;
//			this.lat = lat;
//			this.lon = lon;
//		}
//
//		public static SimpleNode parseFromStream(DataInputStream dis) throws IOException {
//			return new SimpleNode(dis.readLong(),dis.readShort(),dis.readShort());
//		}
//		
//		public void writeToStream(DataOutputStream dos) throws IOException {
//			dos.writeLong(id);
//			dos.writeShort(Short.toUnsignedInt(lat));
//			dos.writeShort(Short.toUnsignedInt(lon));
//		}
//
//		@Override
//		public int compareTo(SimpleNode o) {
//			return Long.compareUnsigned(id, o.id);
//		}
//	}
	// 40 byte
	
	
	
	
	public Cartographer(String mapName) {
		highwayNodesFile = new File(mapName + File.separator + "highwayNodes");	
		pairwiseConnections = new File(mapName + File.separator + "pairwiseConnections");	
		highwayNodesFile.mkdir();
		pairwiseConnections.mkdir();

	}
	
	
	/**
	 * connectionEncoding
	 * 
	 * 1 pedestrian
	 * 1 automobile
	 * 5 speed
	 * 1 cross-segment
	 * 24 ID
	 * 16 distance
	 * 24 latLon (optional, if cross-segment)
	 */

	public static byte[] encodeOffsetData(OffsetData data) {
		int neighboursize = data.getNeighbourList().stream().mapToInt(n -> n.hasLatLonOfSegment() ? 9 : 6).sum();
		byte[] raw = new byte[4+neighboursize];
		ByteBuffer buffer = ByteBuffer.wrap(raw);
		
		buffer.putShort((short) data.getLatOffset());
		buffer.putShort((short) data.getLonOffset());
		
		for(Neighbour n : data.getNeighbourList()) {
			int mask = 0;
			mask|= n.getPedestrian() ? 0x80 : 0;
			mask|= n.getCar() ? 0x40 : 0;
			mask|= ((n.getSpeed() & 0x1f) << 1);
			mask|= n.hasLatLonOfSegment() ? 0x01 : 0;
			
			// write metadata
			buffer.put((byte) mask);
			
			// write ID
			buffer.put((byte) (n.getIdOfNode() >>> 16));
			buffer.putShort((short) n.getIdOfNode());
			
			// distance
			buffer.putShort((short) n.getDistance());
			
			// link to other segment
			if(n.hasLatLonOfSegment()) {
				buffer.put((byte) (n.getLatLonOfSegment() >>> 16));
				buffer.putShort((short) n.getLatLonOfSegment());
			}
		}
		
		return raw;
	}
	

	
	// PairwiseConnection
	// 24 sourceId
	// 16 latOffset
	// 16 lonOffset
	// ConnectionDetails 

	public void writePairwiseConnection(int latLonBase, PairConnection connection) throws IOException {
		
//		if(prevNode.latLonBase != prevToNow.latLon) {
//			System.out.println(ByteUtils.decodeLat(prevNode.latLonBase) +","+ByteUtils.decodeLon(prevNode.latLonBase) + " to " + ByteUtils.decodeLat(prevToNow.latLon) +"," + ByteUtils.decodeLon(prevToNow.latLon));
//		}
//		if(true) {
//			return;
//		}
		DataOutputStream dos = pairwiseConnectionWriters.get(latLonBase);

		
		
		if (dos == null) {
			String pairFileName = pairwiseConnections.getAbsolutePath() + File.separator
					+ latLonBase + ".pairs";
			dos = new DataOutputStream(new BufferedOutputStream(
					new FileOutputStream(new File(pairFileName))));
			pairwiseConnectionWriters.put(latLonBase, dos);
		}

		connection.writeDelimitedTo(dos);
	}




	// class Node {
	// public final short latitude;
	// public final short longitude;
	// public final byte neighbourCount;
	// public final byte[] neighbours;
	//
	// public Node(short latitude, short longitude, byte neighbourCount,
	// byte[] neighbours) {
	// this.latitude = latitude;
	// this.longitude = longitude;
	// this.neighbourCount = neighbourCount;
	// this.neighbours = neighbours;
	// }
	//
	// // Node
	// // short lat
	// // short lon
	// // byte neighbours
	// // byte 1ped, 1car, 6speed;
	// // 2byte lat, lon
	// // 3 byte id
	// byte[] encode() {
	// int byteSize = 5 + Byte.toUnsignedInt(neighbourCount) * 6;
	// byte[] encoded = new byte[byteSize];
	// // write latitude
	// encoded[0] = (byte) (latitude >>> 8);
	// encoded[1] = (byte) latitude;
	// // write longitude
	// encoded[2] = (byte) ((byte) longitude >>> 8);
	// encoded[3] = (byte) longitude;
	// // write neighbour count
	// encoded[4] = neighbourCount;
	// System.arraycopy(neighbours, 0, encoded, 5, neighbours.length);
	// return encoded;
	// }
	//
	// }



//	private final Map<Short, DataOutputStream> highwayNodes = new HashMap<>();

//	public void writeHighwayNode(WayNode n) throws IOException {
//		double positiveLatitude = n.getLatitude() + 120;
//		double positiveLongitude = n.getLongitude() + 180;
//
//		int latitudeBase = (int) Math.floor(positiveLatitude);
//		int longitudeBase = (int) Math.floor(positiveLongitude);
//
//		short latLonBase = ByteUtils.encodeLatLong(latitudeBase, longitudeBase);
//
//		DataOutputStream highwayNodeWriter = getOrCreateStream(latLonBase,highwayNodes);
//		
//		highwayNodeWriter.writeLong(n.getId());
//		
//	}
	
	public void writeSimpleNode(Node node) throws IOException {

		double positiveLatitude = node.getLatitude() + 90;
		double positiveLongitude = node.getLongitude() + 180;

		int latitudeBase = (int) Math.floor(positiveLatitude);
		int longitudeBase = (int) Math.floor(positiveLongitude);

		double fractionalLatitude = positiveLatitude - latitudeBase;
		double fractionalLongitude = positiveLongitude - longitudeBase;

		int latitudeOffset = (int) (fractionalLatitude * Math.pow(2, 16));
		int longitudeOffset = (int) (fractionalLongitude * Math.pow(2, 16));

		int latLonBase = ByteUtils.encodeLatLong(latitudeBase, longitudeBase);

		
		
		
		DataOutputStream dataOutputStream = dataWriters.get(latLonBase);

		if (dataOutputStream == null) {
			String nodeFileName = highwayNodesFile.getAbsolutePath() + File.separator
					+ latLonBase + ".nodes";
			dataOutputStream = new DataOutputStream(new BufferedOutputStream(
					new FileOutputStream(new File(nodeFileName))));
			dataWriters.put(latLonBase, dataOutputStream);
		}
		
		SimpleNode.Builder sNode = SimpleNode.newBuilder();
		sNode.setId(node.getId());
		sNode.setLatOffset(latitudeOffset);
		sNode.setLonOffset(longitudeOffset);
		
		sNode.build().writeDelimitedTo(dataOutputStream);
	}

	@Override
	public void close() throws IOException {
		for (DataOutputStream dos : dataWriters.values()) {
			dos.close();
		}
		for (DataOutputStream dos : pairwiseConnectionWriters.values()) {
			dos.close();
		}
	}



	
}
