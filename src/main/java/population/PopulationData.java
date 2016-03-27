package population;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Locale;
import java.util.Scanner;
import java.util.function.BiConsumer;

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
			
			double minDensity = Integer.MAX_VALUE;
			double maxDensity = Integer.MIN_VALUE;
			

			double densitySum = 0;
			
			double[][] popData = new double[nrows][ncols];
				for (int row = 0; row < nrows; row++) {
					for (int col = 0; col < ncols; col++) {
					popData[row][col] = scan.nextDouble();
					if(popData[row][col] >= 0) {
						minDensity = Math.min(minDensity, popData[row][col]);
						maxDensity = Math.max(maxDensity, popData[row][col]);
						densitySum+=popData[row][col];
					}
				}
			}
				double avgDensity = densitySum / (nrows*ncols);
			return new PopulationData(ncols, nrows, xllcorner, yllcorner,
					cellsize, NODATA_value, popData,minDensity, maxDensity, avgDensity);
		}

	}

	private int ncols;
	private int nrows;
	private int xllcorner;
	private int yllcorner;
	private double cellsize;
	private int NODATA_value;
	private double[][] popData;
	private double minDensity;
	private double maxDensity;
	private double avgDensity;
	
	


	public int getNcols() {
		return ncols;
	}

	public int getNrows() {
		return nrows;
	}

	public int getXllcorner() {
		return xllcorner;
	}

	public int getYllcorner() {
		return yllcorner;
	}

	public double getCellsize() {
		return cellsize;
	}

	public int getNODATA_value() {
		return NODATA_value;
	}

	public double[][] getPopData() {
		return popData;
	}
	
	public double getMinDensity() {
		return minDensity;
	}
	
	public double getMaxDensity() {
		return maxDensity;
	}
	
	public Rectangle2D getCoveredArea() {
		return new Rectangle2D.Float(xllcorner,yllcorner,(float)(cellsize*ncols),(float) (cellsize*nrows));
	}
	
	public void forEachCell(BiConsumer<Point2D, Double> cellConsumer) {
		for (int row = 0; row < nrows; row++) {
			for (int col = 0; col < ncols; col++) {
				cellConsumer.accept(new Point2D.Float((float) (yllcorner +(cellsize*nrows)-(cellsize*row)), (float)( xllcorner +(cellsize*col))), popData[row][col]);
			}
		}
	}
	
	

	public PopulationData(int ncols, int nrows, int xllcorner, int yllcorner,
			double cellsize, int NODATA_value, double[][] popData, double minDensity, double maxDensity, double avgDensity) {
		this.ncols = ncols;
		this.nrows = nrows;
		this.xllcorner = xllcorner;
		this.yllcorner = yllcorner;
		this.cellsize = cellsize;
		this.popData = popData;
		this.NODATA_value = NODATA_value;
		this.minDensity = minDensity;
		this.maxDensity = maxDensity;
		this.avgDensity = avgDensity;
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
		return Math.abs(popData[nrows-row][col] - NODATA_value) > 0.01;
	}
	
	public PopulationInfo closestDataForCoordinate(float lat, float lon) {
		// exclude anything to the left / below leftmost, lowest data point
		if(lat < yllcorner) {
			System.err.printf("lat %f smaller than yllcorner %d\n",lat,yllcorner);
			return null;
		}
		if(lon < xllcorner) {
			System.err.printf("lon %f smaller than xllcorner %d\n",lon,xllcorner);
			return null;
		}
		float latDistance = lat - yllcorner;
		float latGridSteps = (float) (latDistance / cellsize);
		
		float lonDistance = lon - xllcorner;
		float lonGridSteps = (float) (lonDistance / cellsize);
		
		// exclude anything to the right / uppermost data point.
		if(Math.ceil(latGridSteps) >=  nrows) {
			System.err.printf("latgridsteps %d exceeds available grids  %d (too high/ too right)\n",(int)Math.ceil(latGridSteps),nrows);
			return null;
		}
		if(Math.ceil(lonGridSteps) >=  ncols) {
			System.err.printf("longridsteps %d exceeds available grids  %d (too high/ too right)\n",(int)Math.ceil(lonGridSteps),ncols);
			return null;
		}
		// now we excluded any Index-out-of-bounds-problems.
	
		// lower left position in data grid
		int lowerLeftRow = (int) Math.floor(latGridSteps);
		int lowerLeftCol = (int) Math.floor(lonGridSteps);
		
		boolean allRequiredDataPresent = hasDataAt(lowerLeftRow, lowerLeftCol) && hasDataAt(lowerLeftRow+1, lowerLeftCol) && hasDataAt(lowerLeftRow+1, lowerLeftCol+1) && hasDataAt(lowerLeftRow, lowerLeftCol+1);
		
		if(!allRequiredDataPresent) {
			System.err.println("Not all data present");
			return null;
		}
		// lat lon of points in data grid
		float lowerLeftCoordLat = (float) (yllcorner + (lowerLeftRow * cellsize));
		float lowerLeftCoordLon = (float) (xllcorner + (lowerLeftCol * cellsize)); 
		
		
		PopulationPoint leftLower = new PopulationPoint(lowerLeftCoordLat, lowerLeftCoordLon,popData[nrows-lowerLeftRow][lowerLeftCol]);
		PopulationPoint leftUpper = new PopulationPoint(lowerLeftCoordLat + cellsize, lowerLeftCoordLon,popData[nrows-lowerLeftRow+1][lowerLeftCol]);
		PopulationPoint rightUpper = new PopulationPoint(lowerLeftCoordLat + cellsize, lowerLeftCoordLon+ cellsize,popData[nrows-lowerLeftRow+1][lowerLeftCol+1]);
		PopulationPoint rightLower = new PopulationPoint(lowerLeftCoordLat, lowerLeftCoordLon + cellsize ,popData[nrows-lowerLeftRow][lowerLeftCol+1]);
		
		PopulationPoint pointOfInterest = interpolatePopulation(leftUpper, rightUpper, leftLower, rightLower, lat, lon);
		
		
		PopulationInfo info = new PopulationInfo(leftUpper, rightUpper, leftLower, rightLower, pointOfInterest);
		
		return info;
	}

//	
//	public static PopulationPoint interpolatePopulation(PopulationPoint leftUpper,
//			PopulationPoint rightUpper, PopulationPoint leftLower,
//			PopulationPoint rightLower, float lat, float lon) {
//		
//		double xDist = Math.abs(lat - leftUpper.getX());
//		double xFraction = xDist / (rightUpper.getX() - leftUpper.getX());
//
//		double upperXDelta = leftUpper.populationDensity - rightUpper.populationDensity;
//		double xUpperInterpol = leftUpper.populationDensity - (xFraction * upperXDelta);
//		
//		return new PopulationPoint(lat, lon, xUpperInterpol);
//	}
	
	/**
	 * 
	 * Interpolates population. See https://en.wikipedia.org/wiki/Bilinear_interpolation#Unit_Square
	 * 
	 * Normalises lat/lon to be in between 0-1, with regards to the left lowest coordinate (leftLower)
	 * 
	 * @param leftUpper, [0-1]
	 * @param rightUpper, [0-1]
	 * @param leftLower, [0-1]
	 * @param rightLower, [0-1]
	 * @param lat, [0-1]
	 * @param lon, [0-1]
	 * @return
	 */
	public static PopulationPoint interpolatePopulation(PopulationPoint leftUpper,
			PopulationPoint rightUpper, PopulationPoint leftLower,
			PopulationPoint rightLower, float lat, float lon) {
		float cellSize = (float) (rightUpper.getY() - leftUpper.getY());
		
		float savLat = lat;
		float savLon = lon;
		
		lat = (float) (lat - leftLower.getX());
		lon = (float) (lon - leftLower.getY());
		lat/= cellSize;
		lon/= cellSize;
		double interpolated = (leftLower.populationDensity*(1-lat))*(1-lon)+(leftUpper.populationDensity*lat*(1-lon)) + (rightLower.populationDensity*(1-lat)*lon)+(rightUpper.populationDensity*lat*lon);
		
		
		return new PopulationPoint(savLat, savLon, interpolated);
	}

	public double getAvgDensity() {
		return avgDensity;
	}

//	public static PopulationPoint interpolatePopulation(float lat, float lon, PopulationPoint... points) {
//		
//		double sum = 0;
//		double areaSum = 0;
//		for(PopulationPoint p : points) {
//			double area = Math.abs(p.getX() - lat) * Math.abs(p.getY() - lon);
//			areaSum+= area;
//			double pointss = area * p.populationDensity;
//			sum+= pointss;
//		}
//		
//		double interpolatedVal = sum / areaSum;
//		return new PopulationPoint(lat,lon, interpolatedVal );
//	}	
//	
 }
