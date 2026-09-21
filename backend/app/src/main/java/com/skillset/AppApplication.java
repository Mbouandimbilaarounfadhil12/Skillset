package com.skillset;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AppApplication {

    public static void main(String[] args) {
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("java.net.preferIPv4Addresses", "true");
        // Workaround: Java 21 WEPollSelectorImpl creates AF_UNIX sockets via PipeImpl
        // using java.io.tmpdir — 8.3 short paths (GENERA~1) fail on Windows AF_UNIX.
        // Http11Nio2Protocol avoids Selector.open() entirely via IOCP.
        // This tmpdir fix is a belt-and-suspenders safety net.
        String tmpDir = System.getProperty("java.io.tmpdir", "");
        if (tmpDir.contains("~")) {
            System.setProperty("java.io.tmpdir", "C:/Temp/");
        }
        SpringApplication.run(AppApplication.class, args);
    }
}
