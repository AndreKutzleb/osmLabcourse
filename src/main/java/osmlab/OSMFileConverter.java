package osmlab;

import java.awt.BorderLayout;
import java.awt.Container;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JProgressBar;

import osm.preprocessing.DataProcessor;
import osm.preprocessing.PipelineParts;
import osmlab.sink.OsmUtils.TriConsumer;

public class OSMFileConverter {

	
	public static void process(File osmFile) throws IOException {
		
	JProgressBar progressBar;
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
	



		PipelineParts parts = new PipelineParts(osmFile.getAbsolutePath(),progressDisplayer);
		List<DataProcessor> selection = parts.getPipelineSteps();

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

