import java.io.FileNotFoundException;
import java.util.Scanner;


public class Dataprint {
	
	public static void main(String[] args) throws FileNotFoundException {
		PopulationData populationData = PopulationData.parseFromFile("deuds00ag.asc");
		System.out.println(populationData);
	}

}
