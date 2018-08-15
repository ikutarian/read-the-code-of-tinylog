package com.okada.tinylog;

import org.pmw.tinylog.ConsoleLoggingWriter;
import org.pmw.tinylog.Logger;

import java.io.IOException;

public class Application {

    public static void main(String[] args) throws IOException {
        // File logFile = new File("/Users/owen/IdeaProjects/tinylog_log/log.txt");
        // Logger.setWriter(new FileLoggingWriter(logFile));
        Logger.setWriter(new ConsoleLoggingWriter());
        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                for (int n = 0; n < 100; ++n) {
                    Logger.info("Test threading! This is log entry {0}.", n);
                }
            }

        });
        thread.start();
    }
}
