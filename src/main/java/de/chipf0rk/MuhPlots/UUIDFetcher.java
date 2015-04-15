package de.chipf0rk.MuhPlots;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.mojang.api.profiles.HttpProfileRepository;
import com.mojang.api.profiles.Profile;

public class UUIDFetcher {
	private HttpProfileRepository pr;
	
	public UUIDFetcher() {
		this.pr = new HttpProfileRepository("minecraft");
	}
	
	public Map<String, UUID> getUUIDs(String... playerNames) {
		Map<String, UUID> out = new HashMap<String, UUID>();
		Profile[] profiles = pr.findProfilesByNames(playerNames);
		
		for(Profile p : profiles) {
			String name = p.getName();
			String formattedUUID = p.getId().replaceFirst("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})", "$1-$2-$3-$4-$5");
			UUID uuid = UUID.fromString(formattedUUID);
			
			out.put(name, uuid);
		}
		
		return out;
	}
}
