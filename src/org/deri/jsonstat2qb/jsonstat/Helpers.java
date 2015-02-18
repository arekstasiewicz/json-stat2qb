package org.deri.jsonstat2qb.jsonstat;

public class Helpers {

	public static String makeSafeName(String name) {
		return name.toLowerCase().replace(" ", "-").replaceAll("[^A-Za-z0-9]-", "");
	}

}
