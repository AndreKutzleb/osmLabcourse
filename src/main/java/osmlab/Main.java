package osmlab;

import java.awt.BorderLayout;
import java.awt.Container;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JProgressBar;
import javax.swing.border.Border;

import osm.preprocessing.DataProcessor;
import osm.preprocessing.PipelineParts;
import osmlab.sink.OsmUtils.TriConsumer;

public class Main {

	public static final int SEGMENTS = 92520;

	public static void main(String[] args) throws IOException {
		
		TriConsumer<String, Integer, Integer> progressPrinter = (msg, current, of) -> {
			int percent = Math.round((current/(float)of) * 100);
			percent = Math.min(percent, 100);
			percent = Math.max(percent, 0);
			System.out.printf("%s: %d/%d (%d%%)\n",msg,current,of,percent);
		};
		
		JProgressBar progressBar;
		//Where the GUI is constructed:
		progressBar = new JProgressBar(0, 100);
		progressBar.setValue(0);
		progressBar.setStringPainted(true);
		
		
	    JFrame f = new JFrame("OSM converter pipeline");
	    f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	    Container content = f.getContentPane();
	    content.add(progressBar, BorderLayout.NORTH);
	    f.setSize(600, 60);
	    f.setVisible(true);
		
		
		TriConsumer<String, Integer, Integer> progressDisplayer = (msg, current, of) -> {

			if (current == -1 || of == -1) {
				// progress unknown
				String text = String.format("%s", msg);
				progressBar.setIndeterminate(true);
				progressBar.setString(text);

			} else {
				// countable progress
				progressBar.setIndeterminate(false);
				int percent = Math.round((current / (float) of) * 100);
				percent = Math.min(percent, 100);
				percent = Math.max(percent, 0);
				String text = String.format("%s: %d/%d (%d%%)", msg, current,
						of, percent);
				progressBar.setValue(percent);
				progressBar.setString(text);
			}
		};
		

		try (Scanner scan = new Scanner(System.in)) {

			String name = "germany-latest.osm.pbf";
//			String name = "saarland-latest.osm.pbf";

			PipelineParts parts = new PipelineParts(name,progressDisplayer);

			System.out.println("What step to start from?");
			for (int i = 0; i < parts.getPipelineSteps().size(); i++) {
				System.out.println(i
						+ " : "
						+ parts.getPipelineSteps().get(i).getClass()
								.getSimpleName());
			}
			System.out.print("Selection: ");
			int start = scan.nextInt();

			System.out.println("What step to end with?");
			for (int i = start; i < parts.getPipelineSteps().size(); i++) {
				System.out.println(i
						+ " : "
						+ parts.getPipelineSteps().get(i).getClass()
								.getSimpleName());
			}
			System.out.print("Selection: ");
			int stop = scan.nextInt();
			List<DataProcessor> selection = parts.getPipelineSteps().subList(
					start, stop + 1);

		    f.setVisible(true);
		    long before = System.currentTimeMillis();
			for (DataProcessor p : selection) {
				p.process();
			}
			long after = System.currentTimeMillis();
			System.out.println("Processed in " + ((after-before)/1000) + " seconds.");
		    f.dispose();

		}
	}
}
