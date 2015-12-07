package osmlab;

import java.io.File;
import java.io.IOException;

public class Main {

	public static void main(String[] args) throws IOException {
		String name = "saarland-latest.osm.pbf";
				DataPreparer preparer = new DataPreparer();
		preparer.prepareData(new File(name));
	}
}
