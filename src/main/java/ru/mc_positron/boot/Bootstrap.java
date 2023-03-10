package ru.mc_positron.boot;

import cn.nukkit.InterruptibleThread;
import cn.nukkit.Server;
import ru.mc_positron.boot.configuration.FileConfiguration;
import ru.mc_positron.util.ServerKiller;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Log4J2LoggerFactory;
import lombok.extern.log4j.Log4j2;

@Log4j2
public final class Bootstrap {

    public static void main(String[] args) {
        new FileConfiguration();

        System.setProperty("java.net.preferIPv4Stack" , "true");
        System.setProperty("log4j.skipJansi", "false");
        System.getProperties().putIfAbsent("io.netty.allocator.type", "unpooled");

        System.setProperty("leveldb.mmap", "true");

        InternalLoggerFactory.setDefaultFactory(Log4J2LoggerFactory.INSTANCE);
        ResourceLeakDetector.setLevel(ResourceLeakDetector.Level.PARANOID);

        try {
            Server.init(new FileConfiguration());
        } catch (Throwable t) {
            log.throwing(t);
        }

        log.info("Stopping other threads");

        for (Thread thread : java.lang.Thread.getAllStackTraces().keySet()) {
            if (!(thread instanceof InterruptibleThread)) {
                continue;
            }
            log.debug("Stopping {} thread", thread.getClass().getSimpleName());
            if (thread.isAlive()) {
                thread.interrupt();
            }
        }

        ServerKiller killer = new ServerKiller(8);
        killer.start();

        System.exit(0);
    }
}
