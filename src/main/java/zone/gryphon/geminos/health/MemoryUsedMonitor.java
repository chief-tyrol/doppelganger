package zone.gryphon.geminos.health;

import lombok.extern.slf4j.Slf4j;
import zone.dragon.dropwizard.lifecycle.InjectableManaged;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author galen
 */
@Slf4j
public class MemoryUsedMonitor implements InjectableManaged {

    private final Runtime runtime = Runtime.getRuntime();

    private final ExecutorService service = Executors.newSingleThreadExecutor();

    private final AtomicBoolean stopped = new AtomicBoolean(false);

    @Override
    public void start() throws Exception {
        service.submit(() -> {
            try {

                Thread.currentThread().setName("MemoryMonitorThread");

                while (!stopped.get()) {

                    doPrint();

                    synchronized (stopped) {
                        try {
                            stopped.wait(5000);
                        } catch (Exception e) {
                            log.debug("Exception while waiting", e);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Task failed", e);
            }
        });
    }

    @Override
    public void stop() throws Exception {
        stopped.set(true);

        synchronized (stopped) {
            stopped.notifyAll();
        }
        service.shutdown();
        service.awaitTermination(1, TimeUnit.SECONDS);

        // do one final memory usage print before quitting
        doPrint();
    }

    private void doPrint() {
        long free = runtime.freeMemory();
        long total = runtime.totalMemory();
        long used = total - free;
        long max = runtime.maxMemory();
        log.info("Free: {} MB, Total: {} MB, Used: {} MB ({}%), Max: {} MB",
                format(free),
                format(total),
                format(used),
                String.format("%.2f", 100.0 * used / total),
                format(max));
    }

    private String format(long memoryInBytes) {
        return String.format("%.2f", memoryInBytes / 1000.0 / 1000.0);
    }
}
