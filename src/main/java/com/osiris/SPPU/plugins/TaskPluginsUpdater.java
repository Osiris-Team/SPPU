/*
 * Copyright (c) 2021 Osiris-Team.
 * All rights reserved.
 *
 * This software is copyrighted work, licensed under the terms
 * of the MIT-License. Consult the "LICENSE" file for details.
 */

package com.osiris.SPPU.plugins;

import com.osiris.SPPU.Main;
import com.osiris.SPPU.plugins.search.SearchMaster;
import com.osiris.SPPU.plugins.search.SearchResult;
import com.osiris.SPPU.utils.GD;
import com.osiris.betterthread.BetterThread;
import com.osiris.betterthread.BetterThreadManager;
import com.osiris.betterthread.BetterWarning;
import com.osiris.dyml.DYModule;
import com.osiris.dyml.exceptions.DuplicateKeyException;
import com.osiris.headlessbrowser.HBrowser;
import com.osiris.headlessbrowser.windows.PlaywrightWindow;
import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Document;

import java.io.File;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.osiris.SPPU.utils.GD.CONFIG;

public class TaskPluginsUpdater extends BetterThread {
    //private final PluginsUpdateResultConnection con;
    private final String notifyProfile = "NOTIFY";
    private final String manualProfile = "MANUAL";
    private final String automaticProfile = "AUTOMATIC";

    private final List<TaskPluginDownload> downloadTasksList = new ArrayList<>();
    private final List<TaskPremiumSpigotPluginDownload> premiumDownloadTasksList = new ArrayList<>();
    @NotNull
    private final List<DetailedPlugin> includedPlugins = new ArrayList<>();
    @NotNull
    private final List<DetailedPlugin> allPlugins = new ArrayList<>();
    @NotNull
    private final List<DetailedPlugin> excludedPlugins = new ArrayList<>();
    private String userProfile;
    private String pluginsConfigName;
    private int updatesAvailable = 0;

    public TaskPluginsUpdater(String name, BetterThreadManager manager) {
        super(name, manager);
    }


    @Override
    public void runAtStart() throws Exception {
        CONFIG.load(); // No lock needed, since there are no other threads that access this file
        String name = CONFIG.getFileNameWithoutExt();
        DYModule keep_removed = CONFIG.keep_removed;
        DYModule profile = CONFIG.profile;
        this.userProfile = profile.asString();
        DYModule async = CONFIG.async;
        DYModule spigotUsername = CONFIG.spigotUsername;
        DYModule spigotPassword = CONFIG.spigotPassword;
        DYModule spigotUsernameOld = CONFIG.spigotUsernameOld;
        DYModule spigotPasswordOld = CONFIG.spigotPasswordOld;

        PluginManager man = new PluginManager();
        this.allPlugins.addAll(man.getPlugins());
        if (!allPlugins.isEmpty())
            for (DetailedPlugin pl :
                    allPlugins) {
                try {
                    final String plName = pl.getName();
                    if (pl.getName() == null || pl.getName().isEmpty())
                        throw new Exception("The plugins name couldn't be determined for '" + pl.getInstallationPath() + "'!");

                    DYModule exclude = CONFIG.put(name, plName, "exclude").setDefValues("false"); // Check this plugin?
                    DYModule version = CONFIG.put(name, plName, "version").setDefValues(pl.getVersion());
                    DYModule latestVersion = CONFIG.put(name, plName, "latest-version");
                    DYModule author = CONFIG.put(name, plName, "author").setDefValues(pl.getAuthor());
                    DYModule spigotId = CONFIG.put(name, plName, "spigot-id").setDefValues("0");
                    //DYModule songodaId = new DYModule(config, getModules(), name, plName,+".songoda-id", 0); // TODO WORK_IN_PROGRESS
                    DYModule bukkitId = CONFIG.put(name, plName, "bukkit-id").setDefValues("0");
                    DYModule ignoreContentType = CONFIG.put(name, plName, "ignore-content-type").setDefValues("false");
                    DYModule customCheckURL = CONFIG.put(name, plName, "custom-check-url");
                    DYModule customDownloadURL = CONFIG.put(name, plName, "custom-download-url");

                    // The plugin devs can add their spigot/bukkit ids to their plugin.yml files
                    if (pl.getSpigotId() != 0 && spigotId.asString() != null && spigotId.asInt() == 0) // Don't update the value, if the user has already set it
                        spigotId.setValues("" + pl.getSpigotId());
                    if (pl.getBukkitId() != 0 && bukkitId.asString() != null && bukkitId.asInt() == 0)
                        bukkitId.setValues("" + pl.getBukkitId());

                    // Update the detailed plugins in-memory values
                    pl.setSpigotId(spigotId.asInt());
                    pl.setBukkitId(bukkitId.asInt());
                    pl.setIgnoreContentType(ignoreContentType.asBoolean());
                    pl.setCustomLink(customDownloadURL.asString());

                    // Check for missing author in plugin.yml
                    if ((pl.getVersion() == null || pl.getVersion().trim().isEmpty())
                            && (spigotId.asString() == null || spigotId.asInt() == 0)
                            && (bukkitId.asString() == null || bukkitId.asInt() == 0)) {
                        exclude.setValues("true");
                        this.addWarning("Plugin " + pl.getName() + " is missing 'version' in its plugin.yml file and was excluded.");
                    }

                    // Check for missing version in plugin.yml
                    if ((pl.getAuthor() == null || pl.getAuthor().trim().isEmpty())
                            && (spigotId.asString() == null || spigotId.asInt() == 0)
                            && (bukkitId.asString() == null || bukkitId.asInt() == 0)) {
                        exclude.setValues("true");
                        this.addWarning("Plugin " + pl.getName() + " is missing 'author' or 'authors' in its plugin.yml file and was excluded.");
                    }

                    if (!exclude.asBoolean())
                        includedPlugins.add(pl);
                    else
                        excludedPlugins.add(pl);
                } catch (DuplicateKeyException e) {
                    addWarning(new BetterWarning(this, e, "Duplicate plugin '" + pl.getName() + "' (or plugin name from its plugin.yml) found in your plugins directory. " +
                            "Its recommended to remove it."));
                } catch (Exception e) {
                    addWarning(new BetterWarning(this, e));
                }
            }

        if (keep_removed.asBoolean())
            CONFIG.save();
        else {
            CONFIG.save(true); // This overwrites the file and removes everything else that wasn't added via the add method before.
        }
        pluginsConfigName = CONFIG.getFileNameWithoutExt();

        // First we get the latest plugin details from the yml config.
        // The minimum required information is:
        // name, version, and author. Otherwise they won't get update-checked by AutoPlug (and are not inside the list below).
        setStatus("Fetching latest plugin data...");
        int size = includedPlugins.size();
        if (size == 0) throw new Exception("Plugins size is 0! Nothing to check...");
        setMax(size);

        // TODO USE THIS FOR RESULT REPORT
        int sizeSpigotPlugins = 0;
        int sizeBukkitPlugins = 0;
        int sizeCustomLinkPlugins = 0;
        int sizeUnknownPlugins = 0;

        ExecutorService executorService;
        if (async.asBoolean())
            executorService = Executors.newFixedThreadPool(size);
        else
            executorService = Executors.newSingleThreadExecutor();
        List<Future<SearchResult>> activeFutures = new ArrayList<>();
        for (DetailedPlugin pl :
                includedPlugins) {
            try {
                setStatus("Initialising update check for  " + pl.getName() + "...");
                if (pl.getSpigotId() != 0) {
                    sizeSpigotPlugins++; // SPIGOT PLUGIN
                    activeFutures.add(executorService.submit(() -> new SearchMaster().searchBySpigotId(pl)));
                } else if (pl.getBukkitId() != 0) {
                    sizeBukkitPlugins++; // BUKKIT PLUGIN
                    activeFutures.add(executorService.submit(() -> new SearchMaster().searchByBukkitId(pl)));
                } else if (pl.getCustomLink() != null && !pl.getCustomLink().isEmpty()) {
                    sizeCustomLinkPlugins++; // CUSTOM LINK PLUGIN
                    if (pl.getSpigotId() != 0)
                        activeFutures.add(executorService.submit(() -> new SearchMaster().searchBySpigotId(pl)));
                    else if (pl.getBukkitId() != 0)
                        activeFutures.add(executorService.submit(() -> new SearchMaster().searchByBukkitId(pl)));
                    else
                        activeFutures.add(executorService.submit(() -> new SearchMaster().unknownSearch(pl)));
                } else {
                    sizeUnknownPlugins++; // UNKNOWN PLUGIN
                    activeFutures.add(executorService.submit(() -> new SearchMaster().unknownSearch(pl)));
                }
            } catch (Exception e) {
                this.getWarnings().add(new BetterWarning(this, e, "Critical error while searching for update for '" + pl.getName() + "' plugin!"));
            }
        }

        List<SearchResult> updatablePremiumSpigotPlugins = new ArrayList<>();
        List<SearchResult> results = new ArrayList<>();
        while (!activeFutures.isEmpty()) {
            Thread.sleep(250);
            Future<SearchResult> finishedFuture = null;
            for (Future<SearchResult> future :
                    activeFutures) {
                if (future.isDone()) {
                    finishedFuture = future;
                    break;
                }
            }

            if (finishedFuture != null) {
                activeFutures.remove(finishedFuture);
                SearchResult result = finishedFuture.get();
                results.add(result);
                DetailedPlugin pl = result.getPlugin();
                byte code = result.getResultCode();
                String type = result.getDownloadType(); // The file type to download (Note: When 'external' is returned nothing will be downloaded. Working on a fix for this!)
                String latest = result.getLatestVersion(); // The latest version as String
                String downloadUrl = result.getDownloadUrl(); // The download url for the latest version
                String resultSpigotId = result.getSpigotId();
                String resultBukkitId = result.getBukkitId();
                this.setStatus("Checked '" + pl.getName() + "' plugin (" + results.size() + "/" + size + ")");
                if (code == 0 || code == 1) {

                    if (code == 1 && pl.isPremium())
                        updatablePremiumSpigotPlugins.add(result);
                    else
                        doDownloadLogic(result);

                } else if (code == 2)
                    if (result.getException() != null)
                        getWarnings().add(new BetterWarning(this, result.getException(), "There was an api-error for " + pl.getName() + "!"));
                    else
                        getWarnings().add(new BetterWarning(this, new Exception("There was an api-error for " + pl.getName() + "!")));
                else if (code == 3)
                    getWarnings().add(new BetterWarning(this, new Exception("Plugin " + pl.getName() + " was not found by the search-algorithm! Specify an id in the plugins config file.")));
                else
                    getWarnings().add(new BetterWarning(this, new Exception("Unknown error occurred! Code: " + code + "."), "Notify the developers. Fastest way is through discord (https://discord.gg/GGNmtCC)."));

                try {
                    DYModule mSpigotId = CONFIG.get(pluginsConfigName, pl.getName(), "spigot-id");
                    if (resultSpigotId != null
                            && (mSpigotId.asString() == null || mSpigotId.asInt() == 0)) // Because we can get a "null" string from the server
                        mSpigotId.setValues(resultSpigotId);

                    DYModule mBukkitId = CONFIG.get(pluginsConfigName, pl.getName(), "bukkit-id");
                    if (resultBukkitId != null
                            && (mSpigotId.asString() == null || mSpigotId.asInt() == 0)) // Because we can get a "null" string from the server
                        mBukkitId.setValues(resultBukkitId);

                    // The config gets saved at the end of the runAtStart method.
                } catch (Exception e) {
                    getWarnings().add(new BetterWarning(this, e));
                }
            }
        }


        // Wait until all regular download tasks have finished.
        while (!downloadTasksList.isEmpty()) {
            Thread.sleep(1000);
            TaskPluginDownload finishedDownloadTask = null;
            for (TaskPluginDownload task :
                    downloadTasksList) {
                if (!task.isAlive()) {
                    finishedDownloadTask = task;
                    break;
                }
            }

            if (finishedDownloadTask != null) {
                downloadTasksList.remove(finishedDownloadTask);
                SearchResult matchingResult = null;
                for (SearchResult result :
                        results) {
                    if (result.getPlugin().getName().equals(finishedDownloadTask.getPlName())) {
                        matchingResult = result;
                        break;
                    }
                }
                if (matchingResult == null)
                    throw new Exception("This should not happen! Please report to the devs!");

                if (finishedDownloadTask.isDownloadSuccessful())
                    matchingResult.setResultCode((byte) 5);

                if (finishedDownloadTask.isInstallSuccessful())
                    matchingResult.setResultCode((byte) 6);
            }
        }

        // Do premium stuff
        if (!updatablePremiumSpigotPlugins.isEmpty()) {
            setStatus("Logging in to spigotmc.org...");
            OutputStream debugOut = null;
            if (Main.isDEBUG) debugOut = System.out;
            try (PlaywrightWindow window = new HBrowser().openCustomWindow().debugOutputStream(debugOut).temporaryUserDataDir(true).headless(false).buildPlaywrightWindow()) {
                SpigotAuthenticator spigotAuthenticator = new SpigotAuthenticator();
                spigotAuthenticator.attemptLoginForWindow(window, spigotUsername.asString(), spigotPassword.asString(),
                        spigotUsernameOld.asString(), spigotPasswordOld.asString()); // Throws exception on login fail
                spigotUsernameOld.setValues(spigotUsername.asString());
                spigotPasswordOld.setValues(spigotPassword.asString());
                setStatus("Logged in successfully! Updating plugins...");
                for (SearchResult result :
                        updatablePremiumSpigotPlugins) {
                    try {
                        setStatus("Updating premium plugin '" + result.getPlugin().getName() + "' from '" + result.getPlugin().getVersion() + "' to '" + result.getLatestVersion() + "'...");
                        DetailedPlugin pl = result.getPlugin();
                        String latest = result.getLatestVersion();
                        String type = result.getDownloadType();
                        window.newTab("https://www.spigotmc.org/resources/" + result.getSpigotId());
                        spigotAuthenticator.waitForCloudflare(window);
                        Document doc = window.getBodyInnerHtml();
                        String url = "https://www.spigotmc.org/" + doc.getElementsByClass("downloadButton")
                                .get(0).getElementsByTag("a").get(0).attr("href"); // The download or purchase buttons <a> tag with the download link

                        if (url.contains("/purchase"))
                            throw new Exception("Premium update failed, because you do not own this premium plugin.");

                        if (result.getResultCode() == 0) {
                            //getSummary().add("Plugin " +pl.getName()+ " is already on the latest version (" + pl.getVersion() + ")"); // Only for testing right now
                        } else {
                            updatesAvailable++;

                            try {
                                // Update the in-memory config
                                DYModule mLatest = CONFIG.get(pluginsConfigName, pl.getName(), "latest-version");
                                mLatest.setValues(result.getLatestVersion());
                            } catch (Exception e) {
                                getWarnings().add(new BetterWarning(this, e));
                            }

                            if (userProfile.equals(notifyProfile)) {
                                addInfo("NOTIFY: Plugin '" + pl.getName() + "' has an update available (" + pl.getVersion() + " -> " + result.getLatestVersion() + ")");
                            } else {
                                if (result.getDownloadType().equals(".jar") || result.getDownloadType().equals("external")) { // Note that "external" support is kind off random and strongly dependent on what spigot devs are doing
                                    if (userProfile.equals(manualProfile)) {
                                        File cache_dest = new File(GD.SPPU_DOWNLOADS_DIR + pl.getName() + "[" + latest + "].jar");
                                        TaskPremiumSpigotPluginDownload task = new TaskPremiumSpigotPluginDownload(window, "PremiumPluginDownloader", getManager(), pl.getName(), latest, url, userProfile, cache_dest);
                                        premiumDownloadTasksList.add(task);
                                        task.start();
                                    } else {
                                        File oldPl = new File(pl.getInstallationPath());
                                        File dest = new File(GD.WORKING_DIR + "/plugins/" + pl.getName() + "-LATEST-" + "[" + latest + "]" + ".jar");
                                        TaskPremiumSpigotPluginDownload task = new TaskPremiumSpigotPluginDownload(window, "PremiumPluginDownloader", getManager(), pl.getName(), latest, url, userProfile, dest, oldPl);
                                        premiumDownloadTasksList.add(task);
                                        task.start();
                                    }
                                } else
                                    getWarnings().add(new BetterWarning(this, new Exception("Failed to download plugin update(" + latest + ") for " + pl.getName() + " because of unsupported type: " + type)));
                            }
                        }

                    } catch (Exception e) {
                        getWarnings().add(new BetterWarning(this, e, "Premium plugin '" + result.getPlugin().getName() + "' update to '" + result.getLatestVersion() + "' failed!"));
                    }
                }

                // Wait until all download tasks have finished.
                while (!premiumDownloadTasksList.isEmpty()) {
                    Thread.sleep(1000);
                    TaskPremiumSpigotPluginDownload finishedDownloadTask = null;
                    for (TaskPremiumSpigotPluginDownload task :
                            premiumDownloadTasksList) {
                        if (!task.isAlive()) {
                            finishedDownloadTask = task;
                            break;
                        }
                    }

                    if (finishedDownloadTask != null) {
                        premiumDownloadTasksList.remove(finishedDownloadTask);
                        SearchResult matchingResult = null;
                        for (SearchResult result :
                                results) {
                            if (result.getPlugin().getName().equals(finishedDownloadTask.getPlName())) {
                                matchingResult = result;
                                break;
                            }
                        }
                        if (matchingResult == null)
                            throw new Exception("This should not happen! Please report to the devs!");

                        if (finishedDownloadTask.isDownloadSuccessful())
                            matchingResult.setResultCode((byte) 5);

                        if (finishedDownloadTask.isInstallSuccessful())
                            matchingResult.setResultCode((byte) 6);
                    }
                }

            } catch (Exception e) {
                getWarnings().add(new BetterWarning(this, e, "Error during premium plugins updating."));
            }
        }

        CONFIG.save();

        finish("Finished checking all plugins (" + results.size() + "/" + size + ")");
    }

    private void doDownloadLogic(@NotNull SearchResult result) {
        byte code = result.getResultCode();
        @NotNull String type = result.getDownloadType();
        String latest = result.getLatestVersion();
        String url = result.getDownloadUrl();
        DetailedPlugin pl = result.getPlugin();
        if (code == 0) {
            //getSummary().add("Plugin " +pl.getName()+ " is already on the latest version (" + pl.getVersion() + ")"); // Only for testing right now
        } else {
            updatesAvailable++;

            try {
                // Update the in-memory config
                DYModule mLatest = CONFIG.get(pluginsConfigName, pl.getName(), "latest-version");
                mLatest.setValues(latest); // Gets saved later
            } catch (Exception e) {
                getWarnings().add(new BetterWarning(this, e));
            }

            if (userProfile.equals(notifyProfile)) {
                addInfo("NOTIFY: Plugin '" + pl.getName() + "' has an update available (" + pl.getVersion() + " -> " + latest + "). Download url: " + url);
            } else {
                if (type.equals(".jar") || type.equals("external")) { // Note that "external" support is kind off random and strongly dependent on what spigot devs are doing
                    if (!pl.isPremium()) {
                        if (userProfile.equals(manualProfile)) {
                            File cache_dest = new File(GD.SPPU_DOWNLOADS_DIR + pl.getName() + "[" + latest + "].jar");
                            TaskPluginDownload task = new TaskPluginDownload("PluginDownloader", getManager(), pl.getName(), latest, url, pl.getIgnoreContentType(), userProfile, cache_dest);
                            downloadTasksList.add(task);
                            task.start();
                        } else {
                            File oldPl = new File(pl.getInstallationPath());
                            File dest = new File(GD.WORKING_DIR + "/plugins/" + pl.getName() + "-LATEST-" + "[" + latest + "]" + ".jar");
                            TaskPluginDownload task = new TaskPluginDownload("PluginDownloader", getManager(), pl.getName(), latest, url, pl.getIgnoreContentType(), userProfile, dest, oldPl);
                            downloadTasksList.add(task);
                            task.start();
                        }
                    }
                } else
                    getWarnings().add(new BetterWarning(this, new Exception("Failed to download plugin update(" + latest + ") for " + pl.getName() + " because of unsupported type: " + type)));
            }
        }

    }

    /**
     * Returns a list containing only plugins, that contain all the information needed to perform a search. <br>
     * That means, that a plugin must have its name, its authors name and its version in its plugin.yml file.
     */
    @NotNull
    public List<DetailedPlugin> getIncludedPlugins() {
        return includedPlugins;
    }

    @NotNull
    public List<DetailedPlugin> getExcludedPlugins() {
        return excludedPlugins;
    }

    /**
     * Returns a list containing all plugins found in the /plugins directory. <br>
     */
    @NotNull
    public List<DetailedPlugin> getAllPlugins() {
        return allPlugins;
    }

}
