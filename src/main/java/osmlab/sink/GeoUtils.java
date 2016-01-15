package osmlab.sink;

public class GeoUtils {

	/**
	 * From http://stackoverflow.com/a/837957
	 * 
	 * Here is a Java implementation of Haversine formula.
	 * I use this in a project to calculate distance in miles between lat/longs.
	 * 
	 * Adapted for results in meters.
	 * 
	 */
	public static float distFrom(float lat1, float lon1, float lat2, float lon2) {
		double earthRadius = 6371000; // meters
		double dLat = Math.toRadians(lat2 - lat1);
		double dLng = Math.toRadians(lon2 - lon1);
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
				+ Math.cos(Math.toRadians(lat1))
				* Math.cos(Math.toRadians(lat2)) * Math.sin(dLng / 2)
				* Math.sin(dLng / 2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		float dist = (float) (earthRadius * c);

		return dist;
	}

}
