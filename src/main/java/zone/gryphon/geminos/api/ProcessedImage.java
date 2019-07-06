package zone.gryphon.geminos.api;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * @author galen
 */
@Builder
@Data
@Slf4j
public class ProcessedImage {

    private final String file;

    private final float[][] histogram;

    private final float[] hue;

    private final float[] saturation;

    private final float[] value;

    public boolean isGreyscale() {
        float sum = 0;

        for (int i = 0; i < this.getHistogram()[0].length; i++) {
            float tmp = this.getHistogram()[0][i] - this.getHistogram()[1][i];
            sum += (tmp * tmp);
        }

        for (int i = 0; i < this.getHistogram()[0].length; i++) {
            float tmp = this.getHistogram()[1][i] - this.getHistogram()[2][i];
            sum += (tmp * tmp);
        }

        for (int i = 0; i < this.getHistogram()[0].length; i++) {
            float tmp = this.getHistogram()[0][i] - this.getHistogram()[2][i];
            sum += (tmp * tmp);
        }

        float rgbDistance = (float) Math.sqrt(sum);
        return rgbDistance < 0.01;

    }

}
