/*
 * Copyright 2019-2019 Gryphon Zone
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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

    @Valid
    @Min(1)
    private int threadCount = Runtime.getRuntime().availableProcessors() * 2;

}
