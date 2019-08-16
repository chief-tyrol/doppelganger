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

/**
 * @author tyrol
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
