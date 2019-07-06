package zone.gryphon.geminos;

import de.thomaskrille.dropwizard_template_config.TemplateConfigBundle;
import io.dropwizard.Application;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import zone.dragon.dropwizard.HK2Bundle;
import zone.gryphon.geminos.configuration.GeminosResources;
import zone.gryphon.geminos.health.MemoryUsedMonitor;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;

public class GeminosApplication extends Application<GeminosConfiguration> {

    public static void main(final String[] args) throws Exception {
        new GeminosApplication().run(args);
    }

    @Override
    public String getName() {
        return "geminos";
    }

    @Override
    public void initialize(final Bootstrap<GeminosConfiguration> bootstrap) {
        // This ensures there's only ever one HK2 bundle; don't use bootstrap.addBundle(new HK2Bundle<>());
        HK2Bundle.addTo(bootstrap);

        bootstrap.addBundle(new TemplateConfigBundle());

        // TODO: application initialization
    }

    @Override
    public void run(final GeminosConfiguration configuration, final Environment environment) {

        environment.jersey().register(MemoryUsedMonitor.class);

        environment.jersey().register(MainRunner.class);

        environment.jersey().register(new AbstractBinder() {
            @Override
            protected void configure() {
                ThreadPoolExecutor executor = (ThreadPoolExecutor)
                        environment.lifecycle().executorService("geminosProcessingThread-%d")
                        .workQueue(new LinkedBlockingDeque<>())
                        .minThreads(configuration.getThreadCount())
                        .maxThreads(configuration.getThreadCount())
                        .build();

                bind(new GeminosResources(executor));
                bind(configuration.getGeminos());
            }
        });
        // TODO: implement application
    }

    }
