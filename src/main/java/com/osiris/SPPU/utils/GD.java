/*
 * Copyright (c) 2021 Osiris-Team.
 * All rights reserved.
 *
 * This software is copyrighted work, licensed under the terms
 * of the MIT-License. Consult the "LICENSE" file for details.
 */

package com.osiris.SPPU.utils;

import com.osiris.SPPU.Config;

import java.io.File;

/**
 * GlobalData, which is always static and used frequently in this project
 */
public class GD {
    public static File WORKING_DIR;
    public static File PLUGINS_DIR;
    public static File SPPU_DIR;
    public static File SPPU_DOWNLOADS_DIR;
    public static Config CONFIG; // Gets loaded in Main

    static {
        WORKING_DIR = new File(System.getProperty("user.dir"));
        PLUGINS_DIR = new File(WORKING_DIR + "/plugins");
        SPPU_DIR = new File(WORKING_DIR + "/SPPU");
        SPPU_DOWNLOADS_DIR = new File(SPPU_DIR + "/downloads");
        SPPU_DIR.mkdirs();
        SPPU_DOWNLOADS_DIR.mkdirs();
    }

}
