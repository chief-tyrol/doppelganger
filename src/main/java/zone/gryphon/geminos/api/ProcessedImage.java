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

package zone.gryphon.geminos.api;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * @author tyrol
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
