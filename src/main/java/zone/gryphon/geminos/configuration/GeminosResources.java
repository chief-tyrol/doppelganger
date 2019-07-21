package zone.gryphon.geminos.configuration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author tyrol
 */
@Data
@Builder
@AllArgsConstructor
public class GeminosResources {

    private final ThreadPoolExecutor threadPoolExecutor;


}
