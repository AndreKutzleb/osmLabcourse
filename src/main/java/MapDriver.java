import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

import osmlab.Main;

public class MapDriver {

	private final String dataFilePattern;
	private final String offsetFilePattern;

	public MapDriver(String mapFolder, boolean preloadAll) throws NumberFormatException, ClassNotFoundException, IOException {
		String basePattern = mapFolder + File.separator + "data"
				+ File.separator;
		dataFilePattern = basePattern + "%d.data.final";
		offsetFilePattern = basePattern + "%d.data.offset";
		
		File[] files = new File(basePattern).listFiles((f) -> f.getName().endsWith(".data.final"));
		long totalFilesize = 0;
		long totalNodes = 0;
		long before = System.currentTimeMillis();
		for(File f : files) {
			MapSegment fSeg = get(Integer.parseInt(f.getName().substring(0, f.getName().indexOf('.'))));
			totalFilesize+=f.length();
			
			totalNodes+= fSeg.offset.length; 
		}
		long after = System.currentTimeMillis();
	
		long delta = after - before;
		long megabytes = (long) (totalFilesize / Math.pow(2, 20));
		
		System.out.println("Loaded " + files.length + " segments with a total of "+ totalNodes +" nodes in " + delta + " ms. ("+ megabytes +" MB)");
		
	}

	private final MapSegment[] segments = new MapSegment[Main.SEGMENTS];

	class MapSegment {
		public final byte[] data;
		public final int[] offset;
		
		private MapSegment(byte[] dataBuffer, int[] offsetBuffer) {
			data = dataBuffer;
			offset = offsetBuffer;
		}
	}

	public MapSegment get(int latLonBase) throws ClassNotFoundException, IOException {
		MapSegment segment = segments[latLonBase];
		if (segment == null) {
			long before = System.currentTimeMillis();
			segment = loadFromDisk(latLonBase);
			long after = System.currentTimeMillis();
			
			long delta = after - before;
			System.out.println("Loaded " + latLonBase + " in " + delta + " ms.");
			segments[latLonBase] = segment;
		}
		return segment;
	}

	private MapSegment loadFromDisk(int latLonBase) throws IOException, ClassNotFoundException {
		String dataFile = String.format(dataFilePattern, latLonBase);
		String offsetFile = String.format(offsetFilePattern, latLonBase);

		try (ObjectInputStream offsetIn = new ObjectInputStream(
				new FileInputStream(offsetFile));
				ObjectInputStream dataIn = new ObjectInputStream(
						new FileInputStream(dataFile));) {

			int[] offsetBuffer = (int[]) offsetIn.readObject();
			byte[] dataBuffer = (byte[]) dataIn.readObject();

			return new MapSegment(dataBuffer,offsetBuffer);
		}
	}

}
