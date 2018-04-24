/*
 * Copyright (c) 2015 Cryptonomex, Inc., and contributors.
 *
 * The MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.tron.common.utils;

import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RandomGenerator<T> {
  private static long RANDOM_GENERATOR_NUMBER = 2685821657736338717L;

  public List<T> shuffle(List<T> list, long time) {
    long headBlockTimeHi = time << 32;

    for (int i = 0; i < list.size(); i++) {
      long v = headBlockTimeHi + i * RANDOM_GENERATOR_NUMBER;
      v = v ^ (v >> 12);
      v = v ^ (v << 25);
      v = v ^ (v >> 27);
      v = v * RANDOM_GENERATOR_NUMBER;

      int index = (int) (i + v % (list.size() - i));
      if (index < 0 || index >= list.size()) {
        logger.warn("index[" + index + "] is out of range[0," + (list.size() - 1) + "],skip");
        continue;
      }
      T tmp = list.get(index);
      list.set(index, list.get(i));
      list.set(i, tmp);
    }
    return list;
  }

}
