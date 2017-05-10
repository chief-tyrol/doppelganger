package zone.gryphon.geminos;

import io.dropwizard.Configuration;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.validator.constraints.*;
import zone.gryphon.geminos.configuration.GeminosImageReadConfiguration;

import javax.validation.Valid;
import javax.validation.constraints.*;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class GeminosConfiguration extends Configuration {

    @Valid
    @NotNull
    private GeminosImageReadConfiguration geminos;

}
