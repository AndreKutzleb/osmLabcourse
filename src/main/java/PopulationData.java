import java.io.File;
import java.io.FileNotFoundException;
import java.util.Locale;
import java.util.Scanner;

public class PopulationData {

	public static PopulationData parseFromFile(String pathToFile) throws FileNotFoundException {
		try (Scanner scan = new Scanner(new File(pathToFile))) {

			scan.useDelimiter("[\\n\\s]+");
			scan.useLocale(Locale.ENGLISH);

			scan.next();
			int ncols = scan.nextInt();
			scan.next();
			int nrows = scan.nextInt();
			scan.next();
			int xllcorner = scan.nextInt();
			scan.next();
			int yllcorner = scan.nextInt();
			scan.next();
			double cellsize = scan.nextDouble();
			scan.next();
			int NODATA_value = scan.nextInt();
			double[][] popData = new double[nrows][ncols];
				for (int row = 0; row < nrows; row++) {
					for (int col = 0; col < ncols; col++) {
					popData[row][col] = scan.nextDouble();
				}
			}

			return new PopulationData(ncols, nrows, xllcorner, yllcorner,
					cellsize, NODATA_value, popData);
		}

	}

	private int ncols;
	private int nrows;
	private int xllcorner;
	private int yllcorner;
	private double cellsize;
	private int NODATA_value;
	private double[][] popData;

	public PopulationData(int ncols, int nrows, int xllcorner, int yllcorner,
			double cellsize, int NODATA_value, double[][] popData) {
		this.ncols = ncols;
		this.nrows = nrows;
		this.xllcorner = xllcorner;
		this.yllcorner = yllcorner;
		this.cellsize = cellsize;
		this.popData = popData;
		this.NODATA_value = NODATA_value;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		for (int row = 0; row < nrows; row++) {
			for (int col = 0; col < ncols; col++) {
				builder.append(popData[row][col] < 0 ? " " : "#");
			}
			builder.append("\n");
		}
		return builder.toString();
	}
}
