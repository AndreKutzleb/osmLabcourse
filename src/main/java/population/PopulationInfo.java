package population;
import java.awt.geom.Point2D;


public class PopulationInfo {
		
	PopulationPoint leftUpper;
	PopulationPoint rightUpper;
	PopulationPoint leftLower;
	PopulationPoint rightLower;
	PopulationPoint pointOfInterest;
	
	public PopulationInfo(PopulationPoint leftUpper,
			PopulationPoint rightUpper, PopulationPoint leftLower,
			PopulationPoint rightLower, PopulationPoint pointOfInterest) {
		this.leftUpper = leftUpper;
		this.rightUpper = rightUpper;
		this.leftLower = leftLower;
		this.rightLower = rightLower;
		this.pointOfInterest = pointOfInterest;
	}

	public PopulationPoint getLeftUpper() {
		return leftUpper;
	}

	public PopulationPoint getRightUpper() {
		return rightUpper;
	}

	public PopulationPoint getLeftLower() {
		return leftLower;
	}

	public PopulationPoint getRightLower() {
		return rightLower;
	}

	public PopulationPoint getPointOfInterest() {
		return pointOfInterest;
	}
	
	

	
	
}
