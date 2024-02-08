package com.osiris.SPPU;

import com.osiris.SPPU.utils.GD;;
import com.osiris.dyml.Yaml;
import com.osiris.dyml.YamlSection;
import com.osiris.dyml.exceptions.*;

import java.io.IOException;


public class Config extends Yaml {
    public YamlSection keep_removed, profile, async, spigotUsername, spigotPassword, spigotUsernameOld, spigotPasswordOld;

    public Config(String file) throws IOException, DuplicateKeyException, IllegalListException, NotLoadedException, IllegalKeyException, YamlReaderException {
        super(file);
        this.load(); // No lock needed, since there are no other threads that access this file
        String name = this.getFileNameWithoutExt();
        this.put(name).setComments(
                "#######################################################################################################################\n" +
                        "SPPU configuration file.\n" +
                        "#######################################################################################################################\n" +
                        "This file contains detailed information about your installed plugins. It is fetched from each plugins 'plugin.yml' file (located inside their jars).\n" +
                        "The data gets refreshed before performing an update-check. To exclude a plugin from the check set exclude=true.\n" +
                        "If a name/author/version is missing, the plugin gets excluded automatically.\n" +
                        "If there are plugins that weren't found by the search-algorithm, you can add an id (spigot or bukkit) and a custom link (optional & must be a static link to the latest plugin jar).\n" +
                        "spigot-id: Can be found directly in the url. Example URLs id is 78414. Example URL: https://www.spigotmc.org/resources/autoplug-automatic-plugin-updater.78414/\n" +
                        "bukkit-id: Is the 'Project-ID' and can be found on the plugins bukkit site inside of the 'About' box at the right.\n" +
                        "custom-check-url (FEATURE NOT WORKING YET): must link to a yaml or json file that contains at least these fields: name, author, version (of the plugin)\n" +
                        "custom-download-url: must be a static url to the plugins latest jar file" +
                        "If a spigot-id is not given, AutoPlug will try and find the matching id by using its unique search-algorithm (if it succeeds the spigot-id gets set, else it stays 0).\n" +
                        "If both (bukkit and spigot) ids are provided, the spigot-id will be used.\n" +
                        "The configuration for uninstalled plugins wont be removed from this file, but they are automatically excluded from future checks (the exclude value is ignored).\n" +
                        "If multiple authors are provided, only the first author will be used by the search-algorithm.\n" +
                        "Note: Remember, that the values for exclude, version and author get overwritten if new data is available.\n" +
                        "Note for plugin devs: You can add your spigot/bukkit-id to your plugin.yml file.");

        keep_removed = this.put(name, "general", "keep-removed").setDefValues("true")
                .setComments("Keep the plugins entry in this file even after its removal/uninstallation?");
        profile = this.put(name, "general", "profile").setDefValues("AUTOMATIC")
                .setComments("Options to choose from:\n" +
                        "NOTIFY: Shows you a list of updatable plugins with their download urls.\n" +
                        "MANUAL: Download plugins updates into the " + GD.SPPU_DOWNLOADS_DIR + " directory\n" +
                        "AUTOMATIC: Downloads and installs plugins updates directly into /plugins\n");
        async = this.put(name, "general", "async").setDefValues("true")
                .setComments("Check for updates asynchronously? It's faster when enabled, but the logs are more messy.");
        spigotUsername = this.put(name, "general", "spigotmc.org-username").setDefValues("INSERT_USERNAME_HERE");
        spigotPassword = this.put(name, "general", "spigotmc.org-password").setDefValues("INSERT_PASSWORD_HERE");
        spigotUsernameOld = this.put(name, "general", "spigotmc.org-username-old")
                .setComments("Don't insert a value for this, nor change it.",
                        "This stores the older username, so that SPPU knows when you changed the actual username and can login with the new username.");
        spigotPasswordOld = this.put(name, "general", "spigotmc.org-password-old")
                .setComments("Don't insert a value for this, nor change it.",
                        "This stores the older password, so that SPPU knows when you changed the actual password and can login with the new password.");
    }
}
