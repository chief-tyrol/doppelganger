package zone.gryphon.geminos;

import com.google.common.base.Joiner;
import com.google.common.base.Stopwatch;
import com.google.common.base.Supplier;
import com.google.common.base.Throwables;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import zone.dragon.dropwizard.lifecycle.InjectableManaged;
import zone.gryphon.geminos.api.ImageDistance;
import zone.gryphon.geminos.api.ProcessedImage;
import zone.gryphon.geminos.configuration.GeminosImageReadConfiguration;
import zone.gryphon.geminos.configuration.GeminosResources;
import zone.gryphon.geminos.util.ArrayUtils;

import javax.imageio.ImageIO;
import javax.inject.Inject;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * @author tyrol
 */
@Slf4j
@RequiredArgsConstructor(onConstructor_={@Inject})
public class MainRunner implements InjectableManaged {

    @NonNull
    private final GeminosResources resources;

    @NonNull
    private final GeminosImageReadConfiguration config;

    @Override
    public void stop() throws Exception {

    }

    @Override
    public void start() throws Exception {
        Executors.newSingleThreadExecutor().submit(() -> {
            Thread.currentThread().setName("GeminosMainProcessingThread");

            try {
                startInternal();
            } catch (Exception e) {
                log.error("Failed to run geminos", e);
            }
        });
    }


    private void startInternal() throws Exception {


        log.info("Calculating number of images...");
        Stopwatch stopwatch = Stopwatch.createStarted();
        final int numberOfFiles = Files.list(config.getRootFolder().toPath()).mapToInt(file -> 1).sum();
        log.info("Done! ({} milliseconds)", stopwatch.elapsed(TimeUnit.MILLISECONDS));

        log.info("Reading in {} images from {}", numberOfFiles, config.getRootFolder());
        stopwatch = Stopwatch.createStarted();
        AtomicInteger readInImages = new AtomicInteger(0);
        List<ProcessedImage> images = Files.list(config.getRootFolder().toPath())
                .parallel()
                .map(this::processImage)
                .filter(Objects::nonNull)
                .filter(image -> !image.isGreyscale())
                .peek((file) -> {
                    int readIn = readInImages.incrementAndGet();
                    printRateLimitedMessage(() -> String.format("Read in %d of %d images (%.2f%% complete)",
                            readIn, numberOfFiles, 100.0 * readIn / numberOfFiles));
                })
                .limit(config.getImageReadLimit())
                .collect(Collectors.toList());
        stopwatch.stop();
        log.info("Done! Time taken to read in {} images: {} seconds", images.size(), stopwatch.elapsed(TimeUnit.SECONDS));

        final int totalIterations = (images.size() * (images.size() - 1)) / 2;
        final int checkpoint = totalIterations / 1000;
        final List<ImageDistance> distances = new ArrayList<>(totalIterations);

        int idx = 0;

        log.info("Calculating differences between images...");
        stopwatch = Stopwatch.createStarted();
        for (int i = 0; i < images.size(); i++) {
            for (int j = i + 1; j < images.size(); j++) {

                distances.add(null);
                final int finalIdx = idx;
                final int finalI = i;
                final int finalJ = j;
                idx++;

                resources.getThreadPoolExecutor().submit(() -> {
                    try {
                        float distance = calculateDistance(images.get(finalI), images.get(finalJ));

                        distances.set(finalIdx, ImageDistance.builder()
                                .distance(distance)
                                .imageOne(images.get(finalI))
                                .imageTwo(images.get(finalJ))
                                .build());

                        Supplier<String> message =
                                () -> String.format("Processed %d of %d differences (%.2f%% complete)...",
                                        finalIdx, totalIterations, 100.0 * finalIdx / totalIterations);
                        printRateLimitedMessage(message);

                    } catch (Exception e) {
                        log.error("Failed to compute distance between images", e);
                    }

                });
            }
        }
        resources.getThreadPoolExecutor().shutdown();
        resources.getThreadPoolExecutor().awaitTermination(1, TimeUnit.DAYS);

        stopwatch.stop();
        log.info("Done! Time taken to calculate {} distances: {} seconds", distances.size(), stopwatch.elapsed(TimeUnit.SECONDS));

        log.info("Sorting distances...");
        stopwatch = Stopwatch.createStarted();
        distances.sort(ImageDistance::compareTo);
        stopwatch.stop();
        log.info("Done! Time taken to sort distances: {} seconds", stopwatch.elapsed(TimeUnit.SECONDS));


        log.info("Writing file to disk...");
        stopwatch = Stopwatch.createStarted();
        Files.write(new File("tmp.txt").toPath(), () -> new Iterator<CharSequence>() {

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
        });

        Files.write(new File("combine_images.sh").toPath(), () -> new Iterator<CharSequence>() {

            int count = -1;

            private Iterator<ImageDistance> internal = distances.iterator();

            @Override
            public boolean hasNext() {
                return internal.hasNext() && count < 10002;
            }

            @Override
            public CharSequence next() {
                count++;
                if (count == 0) {
                    return "#!/usr/bin/env bash";
                } else if (count == 1) {
                    return "set -x";
                } else {
                    ImageDistance next = internal.next();
                    String file = "match_" + count + ".jpg";
                    String file2 = "match_with_percentage_" + count + ".jpg";
                    return String.format("convert %s %s +append %s; " +
                                    "convert %s -background SkyBlue label:\"distance = %.5f\" -background white -gravity center -append %s; " +
                                    "rm %s",
                            next.getImageOne().getFile(),
                            next.getImageTwo().getFile(),
                            file,
                            file,
                            next.getDistance() * 1000,
                            file2,
                            file);
                }
            }
        });
        stopwatch.stop();

        log.info("Done! took {} seconds", stopwatch.elapsed(TimeUnit.SECONDS));


        System.exit(1);
    }

    private final AtomicReference<Instant> lastPrintTime = new AtomicReference<>(Instant.EPOCH);

    private void printRateLimitedMessage(Supplier<String> message) {
        Instant now = Instant.now();
        Instant existing = lastPrintTime.get();
        if (existing.isBefore(now.minus(2500, ChronoUnit.MILLIS))) {
            if (lastPrintTime.compareAndSet(existing, now)) {
                log.info(message.get());
            }
        }
    }

    private float calculateDistance(ProcessedImage one, ProcessedImage two) {

        double hueDistance = ArrayUtils.weightedDistance(one.getHue(), two.getHue());

        double saturationDistance = ArrayUtils.weightedDistance(one.getSaturation(), two.getSaturation());

        double valueDistance = ArrayUtils.weightedDistance(one.getValue(), two.getValue());

        return (float) Math.sqrt(hueDistance * hueDistance + saturationDistance * saturationDistance + valueDistance * valueDistance);
    }


    private ProcessedImage processImage(Path path) {
        BufferedImage img;
        try {
            img = ImageIO.read(path.toFile());
        } catch (Exception e) {
            log.warn("Failed to read in {}: {}", path, Throwables.getRootCause(e).getMessage());
            return null;
        }

        Raster raster = img.getRaster();

        float[] hue = new float[360];
        float[] saturation = new float[50];
        float[] value = new float[50];

        float[][] rgb = new float[3][64];

        final int w = img.getWidth();
        final int h = img.getHeight();
        long pixels = w * h;

        int[] red = raster.getSamples(0, 0, w, h, 0, (int[]) null);
        processPiece(red, rgb[0]);
        normalize(rgb[0], pixels);

        int[] blue = raster.getSamples(0, 0, w, h, 1, (int[]) null);
        processPiece(blue, rgb[1]);
        normalize(rgb[1], pixels);

        int[] green = raster.getSamples(0, 0, w, h, 2, (int[]) null);
        processPiece(green, rgb[2]);
        normalize(rgb[2], pixels);

        populateHSV(hue, saturation, value, red, blue, green);
        normalize(hue, pixels);
        normalize(saturation, pixels);
        normalize(value, pixels);

        return ProcessedImage.builder()
                .file(path.toString())
                .histogram(rgb)
                .hue(hue)
                .saturation(saturation)
                .value(value)
                .build();
    }

    private void populateHSV(float[] hue, float[] saturation, float[] value, int[] red, int[] green, int[] blue) {

        for (int i = 0; i < red.length; i++) {
            float r = red[i];
            float g = green[i];
            float b = blue[i];

            float computedH;
            float computedS;
            float computedV;

            if (r < 0 || g < 0 || b < 0 || r > 255 || g > 255 || b > 255) {
                throw new IllegalArgumentException("RGB values must be in the range 0 to 255.");
            }

            r /= 255;
            g /= 255;
            b /= 255;

            float minRGB = Math.min(r, Math.min(g, b));
            float maxRGB = Math.max(r, Math.max(g, b));

            // Black-gray-white
            if (minRGB == maxRGB) {
                computedH = 0;
                computedS = 0;
                computedV = minRGB;
            } else {
                // Colors other than black-gray-white:
                float d = (r == minRGB) ? g - b : ((b == minRGB) ? r - g : b - r);
                float h = (r == minRGB) ? 3 : ((b == minRGB) ? 1 : 5);
                computedH = 60 * (h - d / (maxRGB - minRGB));
                computedS = (maxRGB - minRGB) / maxRGB;
                computedV = maxRGB;
            }

            updateHSVValues(hue, saturation, value, computedH, computedS, computedV);
        }
    }

    private void updateHSVValues(float[] hue, float[] saturation, float[] value, float h, float s, float v) {
        hue[(int) (h / (360.0 / hue.length))]++;
        saturation[Math.min((int) (s * saturation.length), saturation.length - 1)]++;
        value[Math.min((int) (v * value.length), value.length - 1)]++;
    }

    private void processPiece(int[] pixelData, float[] array) {
        Arrays.stream(pixelData)
                .map(l -> l / (256 / array.length))
                .forEach(idx -> array[idx]++);
    }

    private void normalize(float[] array, double sum) {
        for (int i = 0; i < array.length; i++) {
            array[i] /= sum;
        }
    }
}
