package osmlab.sink;

import java.awt.Point;

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
	
	public static class FloatPoint {
		public final float lat;
		public final float lon;
		
		public FloatPoint(float lat, float lon) {
			this.lat = lat;
			this.lon = lon;
		}	
	}
	
	public static FloatPoint midPoint(double lat1,double lon1,double lat2,double lon2){

	    double dLon = Math.toRadians(lon2 - lon1);

	    //convert to radians
	    lat1 = Math.toRadians(lat1);
	    lat2 = Math.toRadians(lat2);
	    lon1 = Math.toRadians(lon1);

	    double Bx = Math.cos(lat2) * Math.cos(dLon);
	    double By = Math.cos(lat2) * Math.sin(dLon);
	    double lat3 = Math.atan2(Math.sin(lat1) + Math.sin(lat2), Math.sqrt((Math.cos(lat1) + Bx) * (Math.cos(lat1) + Bx) + By * By));
	    double lon3 = lon1 + Math.atan2(By, Math.cos(lat1) + Bx);
	    
	    return new FloatPoint((float)Math.toDegrees(lat3), (float) Math.toDegrees(lon3));
	}

}
