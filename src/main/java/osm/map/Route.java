package osm.map;

import osm.map.Dijkstra.TravelType;
import osmlab.io.AbstractHighwaySink;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.bytes.ByteList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

public class Route {
	
	public final boolean routeFound;
	private final Dijkstra dijkstra;
	
	public static Route noPath(Dijkstra dijkstra, TravelType travelType) {
		return new Route(dijkstra, new IntArrayList(),new ByteArrayList(),new FloatArrayList(),travelType,false);
	}	

	public final IntList path;
	public final ByteList edgeSpeeds;
	public final FloatList distances;
	public final TravelType travelType;
	
	public Route (Dijkstra dijkstra, IntList path, ByteList edgeSpeeds,FloatList distances,TravelType travelType) {
		this(dijkstra, path,edgeSpeeds,distances,travelType, true);
	}
	
	private Route(Dijkstra dijkstra, IntList path, ByteList edgeSpeeds,FloatList distances, TravelType travelType, boolean routeFound) {
		this.path = path;
		this.edgeSpeeds = edgeSpeeds;
		this.distances = distances;		
		this.travelType = travelType;
		this.routeFound = routeFound;
		this.dijkstra = dijkstra;
	}
	
	public float totalDistance() {
		float sum = 0;
		for(int i = 1; i < path.size(); i++) {
			float distance = dijkstra.getGraph().distance(path.getInt(i), path.getInt(i-1));
			sum+=distance;
		}
	
		return sum;
	}
	
	public float timeTakenInSeconds() {
		float sum = 0;
		for(int i = 1; i < path.size(); i++) {
			sum+= timeInSeconds(edgeSpeeds.get(i-1), path.getInt(i-1), path.getInt(i));
		}
		return sum;
	}
	
	private float timeInSeconds(byte edgeSpeed, int from, int to) {
		
		int kmh;
		
		switch (dijkstra.travelType) {
			case CAR_FASTEST :
			case CAR_FASTEST_FF :
			case CAR_SHORTEST :
			case CAR_SHORTEST_FF :
			case HOP_DISTANCE :
			case HOP_DISTANCE_FF : {
				kmh = AbstractHighwaySink.speedBitsToKmh(edgeSpeed);
				break;

			}
			case PEDESTRIAN :
			case PEDESTRIAN_FF : {
				kmh = 5;
				break;
			}
			default :
				throw new IllegalStateException();
		}
		float secondsPerMeter = 3.6f / kmh;

		float timeTakenInSeconds = dijkstra.getGraph()
				.distance(to, from)
				* secondsPerMeter;

	return timeTakenInSeconds;
	}
		
	
}
