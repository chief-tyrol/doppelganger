package zone.gryphon.geminos.api;

import lombok.Builder;
import lombok.Data;

/**
 * @author galen
 */
@Builder
@Data
public class ProcessedImage {

    private final String file;

    private final float[][] pixelData;

}
