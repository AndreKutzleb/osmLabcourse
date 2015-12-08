import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MapDatabase {

	// private final ByteBuffer[] data = new ByteBuffer[Main.SEGMENTS];
	// private final ByteBuffer[] offset = new ByteBuffer[Main.SEGMENTS];
	//
	// private int currentLonLat = -1;

	public static void main(String[] args) throws IOException,
			ClassNotFoundException {
		Map<Integer, Set<Integer>> neighbours = new HashMap<>();

		File dataFile = new File("saarland-latest" + File.separator + "data"
				+ File.separator + "71355.data.final");
		File offsetFile = new File("saarland-latest" + File.separator + "data"
				+ File.separator + "71355.data.offset");

		int[] offsetBuffer;
		ByteBuffer dataBuffer;


		try (

			ObjectInputStream offsetIn = new ObjectInputStream(
					new FileInputStream(offsetFile));
			ObjectInputStream dataIn = new ObjectInputStream(
					new FileInputStream(dataFile));) {

			offsetBuffer = (int[]) offsetIn.readObject();
			dataBuffer = ByteBuffer.wrap((byte[]) dataIn.readObject());
		}


		
		int count = 0;
		for (int id = 0; id < 100; id++) {
			if (count++ > 100) {
				break;
			}
			int position = offsetBuffer[id];
			dataBuffer.position(position);
			dataBuffer.getInt(); // skip lat/lon offset

			int nextNodeAt = id + 1 == offsetBuffer.length
					? offsetBuffer.length
					: offsetBuffer[id + 1];

			System.out.print(id + " --> ");
			while (dataBuffer.position() < nextNodeAt) {

				boolean crossSegment = (Byte.toUnsignedInt(dataBuffer.get()) & 1) > 0;

				int idConnectedTo = Byte.toUnsignedInt(dataBuffer.get()) << 16
						| Short.toUnsignedInt(dataBuffer.getShort());

				if (crossSegment) {
					dataBuffer.getShort(); // skip latLon
					dataBuffer.get(); // skip latLon
				}

				 System.out.print(idConnectedTo+", ");

				Set<Integer> idNeighbours = neighbours.get(id);
				Set<Integer> otherNeighbours = neighbours.get(idConnectedTo);

				if (idNeighbours == null) {
					idNeighbours = new HashSet<>();
					neighbours.put(id, idNeighbours);
				}

				if (otherNeighbours == null) {
					otherNeighbours = new HashSet<>();
					neighbours.put(idConnectedTo, otherNeighbours);
				}

				idNeighbours.add(idConnectedTo);
				otherNeighbours.add(id);
			}
			System.out.println();

		}

		// for(Integer from : neighbours.keySet()) {
		// System.out.println(from + " -> " + neighbours);
		// }
		System.out.println(neighbours);

	}

}
