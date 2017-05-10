package zone.gryphon.geminos.configuration;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.File;

/**
 * @author galen
 */
@Data
public class GeminosImageReadConfiguration {

    @Valid
    @NotNull
    private File rootFolder;


}
