package com.okada.tinylog;

import org.pmw.tinylog.ConsoleLoggingWriter;
import org.pmw.tinylog.Logger;

public class Application {

    public static void main(String[] args) {
        Logger.setWriter(new ConsoleLoggingWriter());
        Logger.info(new RuntimeException("hell world"));
    }
}
