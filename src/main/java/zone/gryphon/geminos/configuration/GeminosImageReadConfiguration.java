package zone.gryphon.geminos.configuration;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.io.File;

/**
 * @author tyrol
 */
@Data
public class GeminosImageReadConfiguration {

    @Valid
    @NotNull
    private File rootFolder;

    @Min(1)
    @Valid
    private int imageReadLimit = 10000;
}
