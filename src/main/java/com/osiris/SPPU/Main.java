package com.osiris.SPPU;

import com.osiris.SPPU.plugins.TaskPluginsUpdater;
import com.osiris.autoplug.core.logger.AL;
import com.osiris.autoplug.core.logger.LogFileWriter;
import com.osiris.autoplug.core.logger.Message;
import com.osiris.autoplug.core.logger.MessageFormatter;
import com.osiris.betterthread.BetterThread;
import com.osiris.betterthread.BetterThreadDisplayer;
import com.osiris.betterthread.BetterThreadManager;
import com.osiris.betterthread.BetterWarning;
import com.osiris.betterthread.exceptions.JLineLinkException;
import com.osiris.dyml.exceptions.*;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;

import static com.osiris.SPPU.utils.GD.CONFIG;
import static com.osiris.SPPU.utils.GD.SPPU_DIR;

public class Main {

    public static void main(String[] args) throws NotLoadedException, IOException, IllegalKeyException, DuplicateKeyException, DYReaderException, IllegalListException, JLineLinkException, DYWriterException, InterruptedException {
        new AL().start("SPPU", false, new File(SPPU_DIR + "/logs"), false);
        CONFIG = new Config(SPPU_DIR + "/config.yml");
        if (CONFIG.getFile().length() == 0) { // First run, enter setup:
            Scanner scanner = new Scanner(System.in);
            System.out.println("Welcome to SPPU. It looks like this is your first run.");
            System.out.println("Complete the setup to start updating your premium plugins.");
            System.out.println();
            for (String comment :
                    CONFIG.profile.getComments()) {
                System.out.println(comment);
            }
            System.out.println("Enter your desired profiles' name (NOTIFY, MANUAL or AUTOMATIC):");
            CONFIG.profile.setValues(scanner.next());

            System.out.println();
            System.out.println("Enter your username/email used to login to spigotmc.org:");
            CONFIG.spigotUsername.setValues(scanner.next());

            System.out.println();
            System.out.println("Enter your password used to login to spigotmc.org:");
            CONFIG.spigotPassword.setValues(scanner.next());

            CONFIG.save();
            System.out.println();
            System.out.println("Setup completed!");
        }
        BetterThreadManager manager = new BetterThreadManager();
        BetterThreadDisplayer displayer = new BetterThreadDisplayer(manager, "[SPPU]");
        displayer.setShowWarnings(false);
        displayer.setShowDetailedWarnings(false);
        displayer.start();
        new TaskPluginsUpdater("PluginsUpdater", manager).start();

        while (displayer.isAlive()) // Wait until the rest is finished
            Thread.sleep(1000);

        new Main().writeAndPrintResults(manager.getAll());
    }

    private void writeAndPrintResults(List<BetterThread> all) {
        for (BetterThread t :
                all) {
            StringBuilder builder = new StringBuilder();
            if (t.isSuccess()) {
                if (t.getWarnings().size() > 0)
                    builder.append("[OK with " + t.getWarnings().size() + "x WARN]");
                else
                    builder.append("[OK]");
            } else if (t.isSkipped())
                builder.append("[SKIPPED]");
            else
                builder.append("[FAILED with " + t.getWarnings().size() + "x WARN]");

            builder.append("[" + t.getName() + "] ");
            builder.append(t.getStatus());

            LogFileWriter.writeToLog(MessageFormatter.formatForFile(
                    new Message(Message.Type.INFO, builder.toString())));

            for (BetterWarning warning :
                    t.getWarnings()) {
                AL.warn(warning.getException(), warning.getExtraInfo());
            }
        }
    }
}
