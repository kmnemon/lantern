package org.lantern;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.jivesoftware.smack.packet.Presence;
import org.lantern.httpseverywhere.HttpsEverywhere;
import org.lantern.httpseverywhere.HttpsEverywhere.HttpsRuleSet;
import org.littleshoot.util.IoUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * Default class containing configuration settings and data.
 */
public class DefaultConfig implements Config {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final GsonBuilder gb = new GsonBuilder();
    
    public DefaultConfig() {
        final TrustedContactsManager tcm = 
            LanternHub.getTrustedContactsManager();
        gb.registerTypeAdapter(Presence.class, new JsonSerializer<Presence>() {
            @Override
            public JsonElement serialize(final Presence pres, final Type type,
                final JsonSerializationContext jsc) {
                final JsonObject obj = new JsonObject();
                obj.addProperty("user", pres.getFrom());
                obj.addProperty("type", pres.getType().name());
                obj.addProperty("trusted", tcm.isTrusted(pres.getFrom()));
                return obj;
            }
        });
        
    }
    
    @Override
    public String roster() {
        final XmppHandler handler = LanternHub.xmppHandler();
        final Collection<Presence> presences = handler.getPresences();
        final Gson gson = gb.create();
        return gson.toJson(presences);
    }

    @Override
    public String whitelist() {
        log.info("Accessing whitelist");
        final Collection<WhitelistEntry> wl = Whitelist.getWhitelist();
        final Map<WhitelistEntry, Map<String,Object>> all = 
            new LinkedHashMap<WhitelistEntry, Map<String,Object>>();
        for (final WhitelistEntry cur : wl) {
            final Map<String,Object> site = new HashMap<String,Object>();
            site.put("required", Whitelist.required(cur));
            //site.put("httpsRules", 
            //    HttpsEverywhere.getApplicableRuleSets("http://"+cur));
            all.put(cur, site);
        }
        
        return LanternUtils.jsonify(all);
    }
    
    @Override
    public String httpsEverywhere() {
        final Map<String, HttpsRuleSet> rules = HttpsEverywhere.getRules();
        return LanternUtils.jsonify(rules);
    }

    @Override
    public String whitelist(final String body) {
        final ObjectMapper mapper = new ObjectMapper();
        
        try {
            final Map<String, Object> wl = mapper.readValue(body, Map.class);
            for (final Map.Entry<String, Object> entry : wl.entrySet()) {
                final String url = entry.getKey();
                if (!Whitelist.isWhitelisted(url)) {
                    Whitelist.addEntry(url);
                }
            }
        } catch (final JsonParseException e) {
            log.warn("Error generating JSON", e);
        } catch (final JsonMappingException e) {
            log.warn("Error generating JSON", e);
        } catch (final IOException e) {
            log.warn("Error generating JSON", e);
        }
        return whitelist();
    }

    @Override
    public String roster(final String body) {
        return "";
    }

}
