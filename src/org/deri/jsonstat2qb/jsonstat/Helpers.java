package org.deri.jsonstat2qb.jsonstat;

public class Helpers {

	public static String makeSafeName(String name) {
		return name.toLowerCase().replaceAll("[^A-Za-z0-9- ]", "").replace("  ", " ").replace(" ", "-").replace("--", "-");
	}

}
