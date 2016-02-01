package osm.map;

import osm.map.Dijkstra.TravelType;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.bytes.ByteList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

public class Route {
	
	public final boolean routeFound;
	
	public static Route noPath(TravelType travelType) {
		return new Route(new IntArrayList(),new ByteArrayList(),new FloatArrayList(),travelType,false);
	}	

	public final IntList path;
	public final ByteList edgeSpeeds;
	public final FloatList distances;
	public final TravelType travelType;
	
	public Route (IntList path, ByteList edgeSpeeds,FloatList distances,TravelType travelType) {
		this(path,edgeSpeeds,distances,travelType, true);
	}
	
	private Route(IntList path, ByteList edgeSpeeds,FloatList distances, TravelType travelType, boolean routeFound) {
		this.path = path;
		this.edgeSpeeds = edgeSpeeds;
		this.distances = distances;		
		this.travelType = travelType;
		this.routeFound = routeFound;
	}
	
	public double totalDistance() {
		return distances.stream().mapToDouble(x -> x).sum();
	}
}
