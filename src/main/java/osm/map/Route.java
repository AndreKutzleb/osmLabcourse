package osm.map;

import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.bytes.ByteList;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

public class Route {
	
	public static Route noPath() {
		return new Route(new IntArrayList(),new ByteArrayList(),new FloatArrayList());
	}	

	public final IntList path;
	public final ByteList edgeSpeeds;
	public final FloatList distances;
	
	public Route (IntList path, ByteList edgeSpeeds,FloatList distances) {
		this.path = path;
		this.edgeSpeeds = edgeSpeeds;
		this.distances = distances;
	}
	
	public double totalDistance() {
		return distances.stream().mapToDouble(x -> x).sum();
	}
}
