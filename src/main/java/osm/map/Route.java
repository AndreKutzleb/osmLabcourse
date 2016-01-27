package osm.map;

import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.bytes.ByteList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

public class Route {
	
	public static Route noPath() {
		return new Route(new IntArrayList(),new ByteArrayList());
	}
	
	public static Route pathOf(IntList path, ByteList edgeSpeeds) {
		return new Route(path,edgeSpeeds);
	}

	public final IntList path;
	public final ByteList edgeSpeeds;
	
	private Route (IntList path, ByteList edgeSpeeds) {
		this.path = path;
		this.edgeSpeeds = edgeSpeeds;
	}
}
