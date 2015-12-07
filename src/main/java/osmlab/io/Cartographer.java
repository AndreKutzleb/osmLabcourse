package osmlab.io;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;

import osmlab.sink.ByteUtils;

public class Cartographer implements AutoCloseable {

	
	private final File highwayNodesFile;


	// intermediate format:
	// long actualId
	// short lat
	// short lon

	// 40 byte
	public Cartographer(String mapName) {
		highwayNodesFile = new File(mapName + File.separator + "highwayNodes");		
		highwayNodesFile.mkdir();

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

	private final Map<Short, DataOutputStream> dataWriters = new HashMap<>();

	private final Map<Short, DataOutputStream> highwayNodes = new HashMap<>();


	private DataOutputStream getOrCreateStream(short latLonBase,Map<Short, DataOutputStream> source) throws FileNotFoundException {
		DataOutputStream dataOutputStream = source.get(latLonBase);

		if (dataOutputStream == null) {
			String nodeFileName = highwayNodesFile.getAbsolutePath() + File.separator
					+ Short.toUnsignedInt(latLonBase) + ".nodes";
			dataOutputStream = new DataOutputStream(new BufferedOutputStream(
					new FileOutputStream(new File(nodeFileName))));
			source.put(latLonBase, dataOutputStream);
		}
		
		return dataOutputStream;
	}
	
	
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

		double positiveLatitude = node.getLatitude() + 120;
		double positiveLongitude = node.getLongitude() + 180;

		int latitudeBase = (int) Math.floor(positiveLatitude);
		int longitudeBase = (int) Math.floor(positiveLongitude);

		double fractionalLatitude = positiveLatitude - latitudeBase;
		double fractionalLongitude = positiveLongitude - longitudeBase;

		int latitudeOffset = (int) (fractionalLatitude * Math.pow(2, 16));
		int longitudeOffset = (int) (fractionalLongitude * Math.pow(2, 16));

		short latLonBase = ByteUtils.encodeLatLong(latitudeBase, longitudeBase);

		DataOutputStream dataOutputStream = getOrCreateStream(latLonBase,dataWriters);
		

		// Write id
		dataOutputStream.writeLong(node.getId());

		// write latitude and longitude fractions
		dataOutputStream.writeShort(latitudeOffset);
		dataOutputStream.writeShort(longitudeOffset);
	}

	@Override
	public void close() throws IOException {
		for (DataOutputStream dos : dataWriters.values()) {
			dos.close();
		}
		for (DataOutputStream dos : highwayNodes.values()) {
			dos.close();
		}
	}
}
