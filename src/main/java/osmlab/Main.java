package osmlab;

import java.io.File;
import java.io.IOException;

public class Main {
	
	public static final int SEGMENTS = 92520;

	public static void main(String[] args) throws IOException {
		
//		int no = 180 << 9 | 360;
//		System.out.println(Integer.toBinaryString(no));
//		
//		if(true) {
//			return;
//		}
		String name = "saarland-latest.osm.pbf";
		//String name = "saarland-latest.osm.pbf";
				DataPreparer preparer = new DataPreparer();
		preparer.prepareData(new File(name));
	}
}
