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

	public PopulationPoint getPopulationPoint(int i) {
		switch(i) {
			case 0: return getLeftUpper();
			case 1: return getRightUpper();
			case 2: return getLeftLower();
			case 3: return getRightLower();
			case 4: return getPointOfInterest();
			default: throw new IllegalArgumentException();
		}
	}
	
	

	
	
}
