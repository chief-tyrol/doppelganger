package zone.gryphon.geminos.api;

import lombok.Builder;
import lombok.Data;

/**
 * @author galen
 */
@Builder
@Data
public class ImageDistance implements Comparable<ImageDistance> {

    private final float distance;

    private final ProcessedImage imageOne;

    private final ProcessedImage imageTwo;

    @Override
    public int compareTo(ImageDistance o) {
        // negative if this is less than o
        if (o == null) {
            return 1;
        }

        return Float.compare(this.distance, o.distance);
    }
}
