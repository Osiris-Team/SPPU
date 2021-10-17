/*
 * Copyright (c) 2021 Osiris-Team.
 * All rights reserved.
 *
 * This software is copyrighted work, licensed under the terms
 * of the MIT-License. Consult the "LICENSE" file for details.
 */

package com.osiris.SPPU.plugins;

import com.osiris.SPPU.utils.GD;
import com.osiris.autoplug.core.logger.AL;
import com.osiris.betterthread.BetterThread;
import com.osiris.betterthread.BetterThreadManager;
import com.osiris.headlessbrowser.windows.PlaywrightWindow;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class TaskPremiumSpigotPluginDownload extends BetterThread {
    private final String plName;
    private final String plLatestVersion;
    private final String url;
    private final String profile;
    private final File finalDest;
    private final File deleteDest;
    private final boolean isPremium;
    private final PlaywrightWindow window;
    private File dest;
    private boolean isDownloadSuccessful;
    private boolean isInstallSuccessful;

    public TaskPremiumSpigotPluginDownload(PlaywrightWindow window, String name, BetterThreadManager manager,
                                           String plName, String plLatestVersion,
                                           String url, String profile, File finalDest) {
        this(window, name, manager, plName, plLatestVersion, url, profile, finalDest, null);
    }

    /**
     * Performs a plugin installation/download according to the users profile.
     *
     * @param name            this processes name.
     * @param manager         the parent process manager.
     * @param plName          plugin name.
     * @param plLatestVersion plugins latest version.
     * @param url             the download-url.
     * @param profile         the users plugin updater profile. NOTIFY, MANUAL or AUTOMATIC.
     * @param finalDest       the final download destination.
     * @param deleteDest      the file that should be deleted on a successful download. If null nothing gets deleted.
     */
    public TaskPremiumSpigotPluginDownload(PlaywrightWindow window, String name, BetterThreadManager manager,
                                           String plName, String plLatestVersion,
                                           String url, String profile, File finalDest, File deleteDest) {
        this(window, name, manager, plName, plLatestVersion, url, profile, finalDest, deleteDest, false);
    }

    public TaskPremiumSpigotPluginDownload(PlaywrightWindow window, String name, BetterThreadManager manager,
                                           String plName, String plLatestVersion,
                                           String url, String profile, File finalDest, File deleteDest,
                                           boolean isPremium) {
        super(name, manager);
        this.window = window;
        this.plName = plName;
        this.plLatestVersion = plLatestVersion;
        this.url = url;
        this.profile = profile;
        this.finalDest = finalDest;
        this.deleteDest = deleteDest;
        this.isPremium = isPremium;
    }

    @Override
    public void runAtStart() throws Exception {
        super.runAtStart();

        if (profile.equals("NOTIFY")) {
            setStatus("Your profile doesn't allow downloads! Profile: " + profile);
            finish(false);
            return;
        } else if (profile.equals("MANUAL")) {
            download();
            isDownloadSuccessful = true;
        } else {
            download();
            isDownloadSuccessful = true;
            if (finalDest.exists()) finalDest.delete();
            finalDest.createNewFile();
            if (deleteDest != null && deleteDest.exists()) deleteDest.delete();
            Files.copy(dest.toPath(), finalDest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            isInstallSuccessful = true;
            setStatus("Installed update for " + plName + " successfully!");
        }
    }

    public void download() throws Exception {
        File dir = new File(GD.WORKING_DIR + "/autoplug/downloads");
        if (!dir.exists()) dir.mkdirs();

        dest = new File(dir + "/" + plName + "-[" + plLatestVersion + "].jar");
        if (dest.exists()) dest.delete();
        dest.createNewFile();

        final String fileName = dest.getName();
        setStatus("Downloading " + fileName + "...");
        AL.debug(this.getClass(), "Downloading " + fileName + " from: " + url);

        try {
            window.download(url, dest);
        } catch (Exception e) {
            throw e;
        }
    }

    public String getPlName() {
        return plName;
    }

    public String getPlLatestVersion() {
        return plLatestVersion;
    }

    public String getUrl() {
        return url;
    }

    public String getProfile() {
        return profile;
    }

    public File getFinalDest() {
        return finalDest;
    }

    public File getDeleteDest() {
        return deleteDest;
    }

    public File getDownloadDest() {
        return dest;
    }

    public boolean isDownloadSuccessful() {
        return isDownloadSuccessful;
    }

    public boolean isInstallSuccessful() {
        return isInstallSuccessful;
    }
}
