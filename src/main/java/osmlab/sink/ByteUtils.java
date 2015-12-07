package osmlab.sink;

public class ByteUtils {

	public static short encodeLatLong(int lat, int lon) {
		int latBts = ((lat << 1) & 0xfe) << 8;
		int lonBits = ((lon) & 0x1ff);
		
		return (short) (latBts | lonBits); 
	}

	
	
}

