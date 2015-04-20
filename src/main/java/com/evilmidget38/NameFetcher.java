package com.evilmidget38;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

public abstract class NameFetcher implements Callable<Map<UUID, String>> {
	private static final String PROFILE_URL = "https://sessionserver.mojang.com/session/minecraft/profile/";
	private static final JSONParser jsonParser = new JSONParser();
	
	private static Map<UUID, String> nameCache = new HashMap<UUID, String>();
	
	public static Map<UUID, String> getNames(Collection<UUID> uuids) throws Exception {
		Map<UUID, String> out = new HashMap<UUID, String>();
		for(UUID uuid : uuids) {
			out.put(uuid, getName(uuid));
		}
		return out;
	}

	public static String getName(UUID uuid) throws Exception {
		if(nameCache.containsKey(uuid)) {
			return nameCache.get(uuid);
		}
		
		HttpURLConnection connection = (HttpURLConnection) new URL(PROFILE_URL + uuid.toString().replace("-", "")).openConnection();
		JSONObject response = (JSONObject) jsonParser.parse(new InputStreamReader(connection.getInputStream()));
		String name = (String) response.get("name");
		String cause = (String) response.get("cause");
		String errorMessage = (String) response.get("errorMessage");
		if (cause != null && cause.length() > 0) {
			throw new IllegalStateException(errorMessage);
		}

		nameCache.put(uuid, name);
			
		return name;
	}
}