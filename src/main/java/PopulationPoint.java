import java.awt.geom.Point2D;

import osmlab.sink.GeoUtils;

public class PopulationPoint extends Point2D {
	private double x;
	private double y;
	double populationDensity;
	
	public PopulationPoint(double x, double y, double populationDensity) {
		this.x = x;
		this.y = y;
		this.populationDensity = populationDensity;
	}
	
	@Override
	public double getX() {
		return x;
	}

	@Override
	public double getY() {
		return y;
	}

	@Override
	public void setLocation(double x, double y) {
		this.x = x;
		this.y = y;
	}
	
	
}