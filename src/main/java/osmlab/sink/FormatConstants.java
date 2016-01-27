package osmlab.sink;

public class FormatConstants {

	public static final int CONSTANT_NODESIZE = 2; // float lat, float lon, 4 byte each -> 8 byte or 64 bit 

//	germany
//	ways:         
//	highways:     
//	nodes:        
//	highwaynodes: 
//	filesize:     
	
	
	// based on germany analysis, this helps estimating the progress for the steps
	public static final long saarlandSize = 2772728596L;
	public static final float waysPerByte = 36728829f / saarlandSize;
	public static final float highwaysPerByte = 9081688f / saarlandSize; 
	public static final float nodesPerByte = 230496796f / saarlandSize; 
	public static final float highwayNodesPerByte = 51727980f / saarlandSize; 
	

}
