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

package zone.gryphon.geminos.util;

import com.google.common.base.Preconditions;
import lombok.NonNull;

/**
 * @author tyrol
 */
public class ArrayUtils {

    /**
     * Calculate the distance between the two arrays with the given offset.
     * The offset is applied to array 2
     *
     * @param one
     * @param two
     * @param offset
     * @return
     */
    public static float distance(@NonNull float[] one, @NonNull float[] two, int offset) {
        Preconditions.checkArgument(one.length == two.length,
                "Provided arrays must be of equal length! got %d and %d)", one.length, two.length);

        float sum = 0f;

        for (int i = 0; i < one.length; i++) {
            float local = one[i] - two[(i + offset) % two.length];
            sum += (local * local);
        }

        return (float) Math.sqrt(sum);
    }

    public static float weightedDistance(@NonNull float[] one, @NonNull float[] two) {
        Preconditions.checkArgument(one.length == two.length,
                "Provided arrays must be of equal length! got %d and %d)", one.length, two.length);

        final float length = one.length;
        final float halfLength = length / 2;

        // calculate for the hue
        float minDistance = Float.MAX_VALUE;

        for (int i = 0; i < length; i++) {
            float distance = ArrayUtils.distance(one, two, i);

            // http://www.wolframalpha.com/input/?dataset=&i=1+-+((x+-+5)%5E2)%2F25+for+x+from+0+to+10
            // multiplier will vary between 0 and 1
            final float xMinusHalfLength = i - halfLength;
            float multiplier = 1 - ((xMinusHalfLength) * (xMinusHalfLength) / (halfLength * halfLength));

            float weightedDistance = distance * (1 + multiplier);

            if (weightedDistance < minDistance) {
                minDistance = weightedDistance;
            }
        }

        return minDistance;
    }
}
