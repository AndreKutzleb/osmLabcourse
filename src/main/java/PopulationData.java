import java.awt.LinearGradientPaint;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Locale;
import java.util.Scanner;

import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;

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
	private LinearInterpolator interpolator = new LinearInterpolator();


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
				builder.append(hasDataAt(row, col) ? "#" : " ");
			}
			builder.append("\n");
		}
		return builder.toString();
	}
	
	private boolean hasDataAt(int row, int col) {
		return Math.abs(popData[row][col] - NODATA_value) < 0.01;
	}
	
	private Rectangle2D closestDataForCoordinate(float lat, float lon) {
		// exclude anything to the left / below leftmost, lowest data point
		if(lat < yllcorner) {
			return null;
		}
		if(lon < xllcorner) {
			return null;
		}
		float latDistance = lat - yllcorner;
		float latGridSteps = (float) (latDistance / cellsize);
		
		float lonDistance = lon - xllcorner;
		float lonGridSteps = (float) (lonDistance / cellsize);
		
		// exclude anything to the right / uppermost data point.
		if(Math.ceil(latGridSteps) >=  nrows) {
			return null;
		}
		if(Math.ceil(lonGridSteps) >=  ncols) {
			return null;
		}
		// now we excluded any Index-out-of-bounds-problems.
	
		// lower left position in data grid
		int lowerLeftRow = (int) Math.floor(latGridSteps);
		int lowerLeftCol = (int) Math.floor(lonGridSteps);
		
		boolean allRequiredDataPresent = hasDataAt(lowerLeftRow, lowerLeftCol) && hasDataAt(lowerLeftRow+1, lowerLeftCol) && hasDataAt(lowerLeftRow+1, lowerLeftCol+1) && hasDataAt(lowerLeftRow, lowerLeftCol+1);
		
		if(!allRequiredDataPresent) {
			return null;
		}
		// lat lon of points in data grid
		float lowerLeftCoordLat = (float) (yllcorner + (lowerLeftRow * cellsize));
		float lowerLeftCoordLon = (float) (xllcorner + (lowerLeftCol * cellsize)); 
		
		
		PopulationPoint leftLower = new PopulationPoint(lowerLeftCoordLat, lowerLeftCoordLon,popData[lowerLeftRow][lowerLeftCol]);
		PopulationPoint leftUpper = new PopulationPoint(lowerLeftCoordLat + cellsize, lowerLeftCoordLon,popData[lowerLeftRow+1][lowerLeftCol]);
		PopulationPoint rightUpper = new PopulationPoint(lowerLeftCoordLat + cellsize, lowerLeftCoordLon+ cellsize,popData[lowerLeftRow+1][lowerLeftCol+1]);
		PopulationPoint rightLower = new PopulationPoint(lowerLeftCoordLat, lowerLeftCoordLon + cellsize ,popData[lowerLeftRow][lowerLeftCol+1]);
		
		PopulationPoint pointOfInterest = interpolatePopulation(lat, lon, leftUpper, rightUpper, leftLower, rightLower);
		PopulationPoint pointOfInterest2 = interpolatePopulation(leftUpper, rightUpper, leftLower, rightLower, lat, lon);
		
		
		PopulationInfo info = new PopulationInfo(leftUpper, rightUpper, leftLower, rightLower, pointOfInterest);
		
		
		
		// TODO
		return null;
	}

	
	public static PopulationPoint interpolatePopulation(PopulationPoint leftUpper,
			PopulationPoint rightUpper, PopulationPoint leftLower,
			PopulationPoint rightLower, float lat, float lon) {
		
		double xDist = Math.abs(lat - leftUpper.getX());
		double xFraction = xDist / (rightUpper.getX() - leftUpper.getX());

		double upperXDelta = leftUpper.populationDensity - rightUpper.populationDensity;
		double xUpperInterpol = leftUpper.populationDensity - (xFraction * upperXDelta);
		
		return new PopulationPoint(lat, lon, xUpperInterpol);
	}
	
	/**
	 * 
	 * Interpolates population. See https://en.wikipedia.org/wiki/Bilinear_interpolation#Unit_Square
	 * 
	 * @param leftUpper, [0-1]
	 * @param rightUpper, [0-1]
	 * @param leftLower, [0-1]
	 * @param rightLower, [0-1]
	 * @param lat, [0-1]
	 * @param lon, [0-1]
	 * @return
	 */
	public static PopulationPoint interpolatePopulationUnitSquare(PopulationPoint leftUpper,
			PopulationPoint rightUpper, PopulationPoint leftLower,
			PopulationPoint rightLower, float lat, float lon) {
		
		double interpolated = (leftLower.populationDensity*(1-lat))*(1-lon)+(leftUpper.populationDensity*lat*(1-lon)) + (rightLower.populationDensity*(1-lat)*lon)+(rightUpper.populationDensity*lat*lon);
		
		return new PopulationPoint(lat, lon, interpolated);
	}

	public static PopulationPoint interpolatePopulation(float lat, float lon, PopulationPoint... points) {
		
		double sum = 0;
		double areaSum = 0;
		for(PopulationPoint p : points) {
			double area = Math.abs(p.getX() - lat) * Math.abs(p.getY() - lon);
			areaSum+= area;
			double pointss = area * p.populationDensity;
			sum+= pointss;
		}
		
		double interpolatedVal = sum / areaSum;
		return new PopulationPoint(lat,lon, interpolatedVal );
	}	
	
 }
