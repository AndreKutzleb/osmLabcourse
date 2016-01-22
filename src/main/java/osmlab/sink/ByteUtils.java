package osmlab.sink;

public class ByteUtils {

	public static int encodeLatLong(int lat, int lon) {
		int mask = (lat & 0xff) << 9;
		mask |= lon & 0x1ff;
		
		return mask;
	}

	public static int decodeLat(int latLon) {
		return (latLon >> 9) & 0xff;
	}

	public static int decodeLon(int latLon) {
		return latLon & 0x1ff;
	}

	public static double reassembleDouble(int base, int fractions) {
		double fraction = fractions / Math.pow(2, 16);
		return base + fraction;
	}

	public static long concat(int high, int low) {
		return Integer.toUnsignedLong(high) << 32 | Integer.toUnsignedLong(low);
	}
	
	/**
	 * 
	 * @param targetId 0 - 2^27, target in offset-array
	 * @param pedestrian if road can be traversed by pedestrian
	 * @param speed 0-15, speed limit
	 * @return int encoded as [targetID][pedestrian][speed]
	 */
	public static int encodeEdge(int targetId, boolean pedestrian, byte speed) {
		int edge = 0;
		int pedestrianVal = pedestrian ? 1 : 0;
		edge |= (targetId << 5);
		edge |= (pedestrianVal << 4);
		edge |= speed;
		
		return edge;
	}

	public static int decodeNeighbour(int edge) {
		return edge >>> 5;
	}
	
	
}

