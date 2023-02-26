package ru.mc_positron.util;

import java.util.concurrent.TimeUnit;

public class ServerKiller extends Thread {

    public final long sleepTime;

    public ServerKiller(long time) {
        this(time, TimeUnit.SECONDS);
    }

    public ServerKiller(long time, TimeUnit unit) {
        sleepTime = unit.toMillis(time);
        setName("Server Killer");
    }

    @Override
    public void run() {
        try {
            sleep(sleepTime);
        } catch (InterruptedException ignored) {

        }

        System.out.println("\nTook too long to stop, server was killed forcefully!\n");
        System.exit(1);
    }
}
