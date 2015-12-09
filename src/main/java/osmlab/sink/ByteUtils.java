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
	
	
}

