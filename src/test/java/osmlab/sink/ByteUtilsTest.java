package osmlab.sink;

import static org.junit.Assert.*;
import junit.framework.Assert;

import org.junit.Test;

public class ByteUtilsTest {

	@Test
	public void test() {
		for(int lat = 0; lat < 120; lat++) {
			for (int lon = 0; lon < 360; lon++) {
				int encodeLatLong = ByteUtils.encodeLatLong(lat, lon);
				int latDecoded = ByteUtils.decodeLat(encodeLatLong);
				int lonDecoded = ByteUtils.decodeLon(encodeLatLong);
				
				assertEquals(lon, lonDecoded);
				assertEquals(lat, latDecoded);
			}
		}
	}

}
