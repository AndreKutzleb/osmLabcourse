package osmlab.io;
import java.util.regex.Pattern;

import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;

public abstract class AbstractHighwaySink extends SimpleSink {

	@Override
	public void process(EntityContainer entityContainer) {
		Entity entity = entityContainer.getEntity();
		if (entity instanceof Way) {

			Way way = (Way) entity;

			HighwayInfos infos = extractHighwayInfos(way);

			if (way.getWayNodes().size() > 1 && infos != null) {
				handleHighway(way, infos);
			}

			handleWay(way);

		} else if (entity instanceof Node) {
			Node node = (Node) entity;
			handleNode(node);
		}
	}
	public void handleWay(Way way) {
	};

	public void handleHighway(Way way, HighwayInfos infos) {
	};

	public void handleNode(Node node) {
	};

	// Original highway parse code by Jonas Grunert

	private static HighwayInfos extractHighwayInfos(Way way) {
		String highway = null;
		String maxspeed = null;
		String sidewalk = null;
		String oneway = null;
		String access = null;

		for (Tag tag : way.getTags()) {
			switch (tag.getKey()) {
				case "highway" :
					highway = tag.getValue();
					break;
				case "maxspeed" :
					maxspeed = tag.getValue();
					break;
				case "sidewalk" :
					sidewalk = tag.getValue();
					break;
				case "oneway" :
					oneway = tag.getValue();
					break;
				case "access" :
					access = tag.getValue();
					break;
				default :
					break;
			}
		}

		boolean accessOk;
		if (access != null
				&& (access.equals("private") || access.equals("no")
						|| access.equals("emergency")
						|| access.equals("agricultural") || access
							.equals("bus"))) {
			accessOk = false;
		} else {
			accessOk = true;
		}
		HighwayInfos hw = null;
		try {
			if (highway != null && accessOk) {
				hw = evaluateHighway(highway, maxspeed, sidewalk, oneway);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return hw;
	}

	public static int UNLIMITED = 300;

	public static int speedBitsToKmh(int speedBits) {

		switch (speedBits) {
		// pedestrian only
			case 0 :
				return 0;
				// walking speed
			case 1 :
				return 5;
				// unlimited
			case 15 :
				return 300;
				// 2-14 -> 10 - 130 kmh (10kmh steps)
			default :
				return (speedBits - 1) * 10;
		}

	}

	final static int[] categories = new int[]{0, 5, 10, 20, 30, 40, 50, 60, 70,
			80, 90, 100, 110, 120, 130, UNLIMITED};

	private static Byte parseSpeed(String maxspeedTag, boolean mph) {
		int maxSpeed;
		try {
			maxSpeed = Integer.parseInt(maxspeedTag);
			
		} catch (NumberFormatException nfe) {
			System.err.println("Malformed input, defaulting to 50 kmh for \""+maxspeedTag+"\"" );
			maxSpeed = 50;
		}

		if (mph) {
			maxSpeed = Math.round(maxSpeed * 1.60934f);
		}

		byte speedBits = -1;
		for (int i = 1; i < categories.length; i++) {
			if (Math.abs(categories[i - 1] - maxSpeed) < (Math
					.abs(categories[i] - maxSpeed))) {
				speedBits = (byte) (i - 1);
				break;
			}
		}
		// in case the speedbits are still 0, the value is closer to
		// unlimited(300) than 130. in this case, use
		// unlimited
		if (speedBits == -1) {
			speedBits = 15;
		}

		return speedBits;
	}
	// Code by Jonas Grunert
	private static HighwayInfos evaluateHighway(String highwayTag,
			String maxspeedTag, String sidewalkTag, String onewayTag) {

		// Try to find out maxspeed
		Byte maxSpeed;
		if (maxspeedTag != null) {
			if (maxspeedTag.equals("none") || maxspeedTag.equals("signals")
					|| maxspeedTag.equals("variable")
					|| maxspeedTag.equals("unlimited")) {
				// No speed limitation
				maxSpeed = 15;
			} else if (maxspeedTag.contains("living_street")) {
				maxSpeed = 1;
			} else if (maxspeedTag.contains("walk")
					|| maxspeedTag.contains("foot")) {
				maxSpeed = 0;
			} else if (maxspeedTag.contains("DE:motorway")) {
				maxSpeed = 15;
			} else if (maxspeedTag.contains("DE:rural")) {
				maxSpeed = 11;
			} else if (maxspeedTag.contains("DE:urban")) {
				maxSpeed = 6;
			} else if (maxspeedTag.contains("DE:walk")) {
				maxSpeed = 1;
			} else if (maxspeedTag.contains("DE:zone")) {
				maxSpeed = 4;
			} else if (maxspeedTag.contains("moderat")) {
				maxSpeed = 6;
			}  else if(maxspeedTag.contains("Spielstrasse")){
				maxSpeed = 1;
	}else {
				try {
					boolean mph = false;
					// Try to parse speed limit
					if (maxspeedTag.contains("mph")) {
						mph = true;
						maxspeedTag = maxspeedTag.replace("mph", "");
					}
					if (maxspeedTag.contains("km/h"))
						maxspeedTag = maxspeedTag.replace("km/h", "");
					if (maxspeedTag.contains("kmh"))
						maxspeedTag = maxspeedTag.replace("kmh", "");
					if (maxspeedTag.contains("."))
						maxspeedTag = maxspeedTag.split("\\.")[0];
					if (maxspeedTag.contains(","))
						maxspeedTag = maxspeedTag.split(",")[0];
					if (maxspeedTag.contains(";"))
						maxspeedTag = maxspeedTag.split(";")[0];
					if (maxspeedTag.contains("-"))
						maxspeedTag = maxspeedTag.split("-")[0];
					if (maxspeedTag.contains(" "))
						maxspeedTag = maxspeedTag.split(" ")[0];

					maxSpeed = parseSpeed(maxspeedTag, mph);

				} catch (Exception e) {
					e.printStackTrace();
					maxSpeed = null;
				}
			}
		} else {
			maxSpeed = null;
		}

		// Try to find out if has sidewalk
		SidewalkMode sidewalk = SidewalkMode.Unspecified;
		if (sidewalkTag != null) {
			if (sidewalkTag.equals("no") || sidewalkTag.equals("none")) {
				sidewalk = SidewalkMode.No;
			} else {
				sidewalk = SidewalkMode.Yes;
			}
		}

		// Try to find out if is oneway
		Boolean oneway;
		if (onewayTag != null) {
			oneway = onewayTag.equals("yes");
		} else {
			oneway = null;
		}

		// Try to classify highway
		if (highwayTag.equals("track")) {
			// track
			if (maxSpeed == null)
				maxSpeed = 2;
			if (oneway == null)
				oneway = false;

			return new HighwayInfos(true, true, oneway, maxSpeed);
		} else if (highwayTag.equals("residential")) {
			// residential road
			if (maxSpeed == null)
				maxSpeed = 6;
			if (oneway == null)
				oneway = false;

			// Residential by default with sideway
			return new HighwayInfos(true, (sidewalk != SidewalkMode.No),
					oneway, maxSpeed);
		} else if (highwayTag.equals("service")) {
			// service road
			if (maxSpeed == null)
				maxSpeed = 4;
			if (oneway == null)
				oneway = false;

			// Residential by default with sideway
			return new HighwayInfos(true, (sidewalk != SidewalkMode.No),
					oneway, maxSpeed);
		} else if (highwayTag.equals("footway") || highwayTag.equals("path")
				|| highwayTag.equals("steps") || highwayTag.equals("bridleway")
				|| highwayTag.equals("pedestrian")) {
			// footway etc.
			if (maxSpeed == null)
				maxSpeed = 0;
			if (oneway == null)
				oneway = false;

			return new HighwayInfos(false, true, oneway, maxSpeed);
		} else if (highwayTag.startsWith("primary")) {
			// country road etc
			if (maxSpeed == null)
				maxSpeed = 11;
			if (oneway == null)
				oneway = false;

			return new HighwayInfos(true, (sidewalk == SidewalkMode.Yes),
					oneway, maxSpeed);
		} else if (highwayTag.startsWith("secondary")
				|| highwayTag.startsWith("tertiary")) {
			// country road etc
			if (maxSpeed == null)
				maxSpeed = 11;
			if (oneway == null)
				oneway = false;

			return new HighwayInfos(true, (sidewalk != SidewalkMode.No),
					oneway, maxSpeed);
		} else if (highwayTag.equals("unclassified")) {
			// unclassified (small road)
			if (maxSpeed == null)
				maxSpeed = 6;
			if (oneway == null)
				oneway = false;

			return new HighwayInfos(true, (sidewalk != SidewalkMode.No),
					oneway, maxSpeed);
		} else if (highwayTag.equals("living_street")) {
			// living street
			if (maxSpeed == null)
				maxSpeed = 1;
			if (oneway == null)
				oneway = false;

			return new HighwayInfos(true, true, oneway, maxSpeed);
		} else if (highwayTag.startsWith("motorway")) {
			// track
			if (maxSpeed == null)
				maxSpeed = 15;
			if (oneway == null)
				oneway = true;

			return new HighwayInfos(true, (sidewalk == SidewalkMode.Yes),
					oneway, maxSpeed);
		} else if (highwayTag.startsWith("trunk")) {
			// trunk road
			if (maxSpeed == null)
				maxSpeed = 15;
			if (oneway == null)
				oneway = false;

			return new HighwayInfos(true, (sidewalk == SidewalkMode.Yes),
					oneway, maxSpeed);
		}

		// Ignore this road if no useful classification available
		return null;
	}

	enum SidewalkMode {
		Yes, No, Unspecified
	};

	public static class HighwayInfos {

		public final boolean Oneway;
		public final boolean pedestrian;
		public final byte MaxSpeed;

		private HighwayInfos(boolean car, boolean pedestrian, boolean oneway,
				byte maxSpeed) {

			this.pedestrian = pedestrian;
			this.Oneway = oneway;
			this.MaxSpeed = maxSpeed;
		}

	}

}