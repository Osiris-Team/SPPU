/*
 * Copyright (c) 2021 Osiris-Team.
 * All rights reserved.
 *
 * This software is copyrighted work, licensed under the terms
 * of the MIT-License. Consult the "LICENSE" file for details.
 */

package com.osiris.SPPU.plugins.search.spigot;

import com.google.gson.JsonObject;
import com.osiris.SPPU.plugins.DetailedPlugin;
import com.osiris.SPPU.plugins.search.SearchResult;
import com.osiris.SPPU.utils.UtilsVersion;
import com.osiris.autoplug.core.json.JsonTools;
import com.osiris.autoplug.core.logger.AL;

public class SpigotSearchById {

    public SearchResult search(DetailedPlugin plugin) {
        int spigotId = plugin.getSpigotId();
        Exception exception = null;

        String url = "https://api.spiget.org/v2/resources/" + spigotId +
                "/versions?size=1&sort=-releaseDate";
        AL.debug(this.getClass(), "[" + plugin.getName() + "] Fetching latest release... (" + url + ")");
        String latest = null;
        String type = null;
        String downloadUrl = null;
        byte code = 0;
        boolean isPremium = false;
        try {
            // Get the latest version
            latest = new JsonTools().getJsonArray(url).get(0).getAsJsonObject().get("name").getAsString();

            // Get the file type and downloadUrl
            String url1 = "https://api.spiget.org/v2/resources/" + spigotId;
            AL.debug(this.getClass(), "[" + plugin.getName() + "] Fetching resource details... (" + url1 + ")");
            JsonObject json = new JsonTools().getJsonObject(url1).getAsJsonObject("file");
            isPremium = Boolean.parseBoolean(new JsonTools().getJsonObject(url1).get("premium").getAsString());
            type = json.get("type").getAsString();
            downloadUrl = "https://www.spigotmc.org/" + json.get("url").getAsString();

            // If not external download over the spiget api
            downloadUrl = "https://api.spiget.org/v2/resources/" + spigotId + "/download";

            if (latest != null && new UtilsVersion().compare(plugin.getVersion(), latest))
                code = 1;
        } catch (Exception e) {
            exception = e;
            code = 2;
        }

        AL.debug(this.getClass(), "[" + plugin.getName() + "] Finished check with results: code:" + code + " latest:" + latest + " downloadURL:" + downloadUrl + " type:" + type + " ");
        SearchResult result = new SearchResult(plugin, code, latest, downloadUrl, type, "" + spigotId, null, isPremium);
        result.setException(exception);
        return result;
    }
}
