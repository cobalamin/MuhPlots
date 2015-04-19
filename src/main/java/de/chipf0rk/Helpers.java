package de.chipf0rk;

import java.util.Collection;
import java.util.Iterator;

public abstract class Helpers {
	public static String join(Collection<?> col, String delim) {
		StringBuilder sb = new StringBuilder();
		Iterator<?> it = col.iterator();
		if (it.hasNext())
			sb.append(it.next().toString());
		while (it.hasNext()) {
			sb.append(delim);
			sb.append(it.next().toString());
		}
		return sb.toString();
	}
}
