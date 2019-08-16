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

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;

import java.util.Arrays;
import java.util.stream.IntStream;

import static org.junit.Assert.*;

/**
 * @author tyrol
 */
@Slf4j
public class MainRunnerTest {

    @Test
    public void test() {
        final double[] buckets = new double[65];
        for (int i = 0; i < buckets.length; i++) {
            buckets[i] = 0;
        }

        IntStream.range(0, 256).forEach(original -> {
            int newBucket = original / 4;
            double remainder = (original / 4.0) % 1;
            buckets[newBucket] += 1 ;

            log.info("original: {}, bucket[{}]: {}, bucket[{}]: {}", original, newBucket, remainder, newBucket + 1, buckets[newBucket + 1]);
        });

        log.info("{}", Arrays.toString(buckets));
    }

}