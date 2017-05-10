package zone.gryphon.geminos;

import com.google.common.base.Joiner;
import com.google.common.base.Stopwatch;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import zone.dragon.dropwizard.lifecycle.InjectableManaged;
import zone.gryphon.geminos.api.ImageDistance;
import zone.gryphon.geminos.api.ProcessedImage;
import zone.gryphon.geminos.configuration.GeminosImageReadConfiguration;

import javax.imageio.ImageIO;
import javax.inject.Inject;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author galen
 */
@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Inject}))
public class MainRunner implements InjectableManaged {

    @NonNull
    private GeminosImageReadConfiguration config;

    @Override
    public void start() throws Exception {
        Stopwatch stopwatch = Stopwatch.createStarted();
        List<ProcessedImage> images = Files.list(config.getRootFolder().toPath())
                .map(this::processImage)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        stopwatch.stop();

        log.info("Time taken to read in {} images: {} seconds", images.size(), stopwatch.elapsed(TimeUnit.SECONDS));

        List<ImageDistance> distances = new ArrayList<>(images.size() * images.size());

        stopwatch = Stopwatch.createStarted();
        for (int i = 0; i < images.size(); i++) {
            for (int j = i + 1; j < images.size(); j++) {
                float distance = calculateDistance(images.get(i), images.get(j));

                distances.add(ImageDistance.builder()
                .distance(distance)
                .imageOne(images.get(i))
                .imageTwo(images.get(j))
                .build());
            }

            if (i % 100 == 0) {
                log.info("Processed another 10000 images (iteration {} of {})", i, images.size());
            }
        }
        stopwatch.stop();
        log.info("Time taken to calculate {} distances: {} seconds", distances.size(), stopwatch.elapsed(TimeUnit.SECONDS));

        log.info("Sorting distances");
        stopwatch = Stopwatch.createStarted();
        distances.sort(ImageDistance::compareTo);
        stopwatch.stop();
        log.info("Time taken to sort distances: {} seconds", stopwatch.elapsed(TimeUnit.SECONDS));


        Files.write(new File("tmp.txt").toPath(), new Iterable<CharSequence>() {

            @Override
            public Iterator<CharSequence> iterator() {
                return new Iterator<CharSequence>() {

                    int count = 0;

                    private Iterator<ImageDistance> internal = distances.iterator();

                    @Override
                    public boolean hasNext() {
                        return internal.hasNext() && count < 10000;
                    }

                    @Override
                    public CharSequence next() {
                        ImageDistance next = internal.next();
                        count++;
                        return Joiner.on(", ").join(next.getDistance(), next.getImageOne().getFile(), next.getImageTwo().getFile());
                    }
                };
            }
        });


        log.info("Done!");
    }

    private float calculateDistance(ProcessedImage one, ProcessedImage two) {

        float sum = 0;

        for (int channel = 0; channel < one.getPixelData().length; channel++) {
            for (int i = 0; i < one.getPixelData()[channel].length; i++) {
                float tmp = one.getPixelData()[channel][i] - two.getPixelData()[channel][i];
                sum += (tmp * tmp);
            }
        }

        return (float) Math.sqrt(sum);
    }


    @Override
    public void stop() throws Exception {

    }

    private ProcessedImage processImage(Path path) {
        BufferedImage img;
        try {
            img = ImageIO.read(path.toFile());
        } catch (IOException e) {
            log.error("Failed to read in image", e);
            return null;
        }

        float[][] rgb = new float[3][256];

        Raster raster = img.getRaster();
        final int w = img.getWidth();
        final int h = img.getHeight();
        long pixels = w * h;
        double[] r = new double[w * h];

        for (int channel = 0; channel < 3; channel++) {
            r = raster.getSamples(0, 0, w, h, channel, r);
            processPiece(r, rgb[channel]);

            for (int j = 0; j < rgb[channel].length; j++) {
                rgb[channel][j] /= pixels;
            }
        }

        return ProcessedImage.builder()
                .file(path.toString())
                .pixelData(rgb)
                .build();
    }

    private void processPiece(double[] pixelData, float[] array) {
        Arrays.stream(pixelData).mapToLong(Math::round).forEach(idx -> array[(int) idx]++);
    }
}
