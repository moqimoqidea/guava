/*
 * Copyright (C) 2011 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.common.hash;

import static com.google.common.hash.Hashing.md5;
import static com.google.common.hash.Hashing.sha1;
import static com.google.common.hash.Hashing.sha256;
import static com.google.common.hash.Hashing.sha384;
import static com.google.common.hash.Hashing.sha512;
import static com.google.common.truth.Truth.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import junit.framework.TestCase;
import org.jspecify.annotations.NullUnmarked;

/**
 * Tests for the MessageDigestHashFunction.
 *
 * @author Kurt Alfred Kluever
 */
@NullUnmarked
public class MessageDigestHashFunctionTest extends TestCase {
  private static final ImmutableSet<String> INPUTS = ImmutableSet.of("", "Z", "foobar");

  // From "How Provider Implementations Are Requested and Supplied" from
  // https://docs.oracle.com/en/java/javase/25/security/java-cryptography-architecture-jca-reference-guide.html#:~:text=How%20Provider%20Implementations%20Are%20Requested%20and%20Supplied
  //  - Some providers may choose to also include alias names.
  //  - For example, the "SHA-1" algorithm might be referred to as "SHA1".
  //  - The algorithm name is not case-sensitive.
  @SuppressWarnings("deprecation") // We still need to test our deprecated APIs.
  private static final ImmutableMap<String, HashFunction> ALGORITHMS =
      new ImmutableMap.Builder<String, HashFunction>()
          .put("MD5", md5())
          .put("SHA", sha1()) // Not the official name, but still works
          .put("SHA1", sha1()) // Not the official name, but still works
          .put("sHa-1", sha1()) // Not the official name, but still works
          .put("SHA-1", sha1())
          .put("SHA-256", sha256())
          .put("SHA-384", sha384())
          .put("SHA-512", sha512())
          .build();

  public void testHashing() throws Exception {
    for (String stringToTest : INPUTS) {
      for (String algorithmToTest : ALGORITHMS.keySet()) {
        assertMessageDigestHashing(HashTestUtils.ascii(stringToTest), algorithmToTest);
      }
    }
  }

  public void testPutAfterHash() {
    Hasher hasher = sha512().newHasher();

    assertThat(
            hasher
                .putString("The quick brown fox jumps over the lazy dog", UTF_8)
                .hash()
                .toString())
        .isEqualTo(
            "07e547d9586f6a73f73fbac0435ed76951218fb7d0c8d788a309d785436bbb642e93a252a954f23912547d1e8a3b5ed6e1bfd7097821233fa0538f3db854fee6");
    assertThrows(IllegalStateException.class, () -> hasher.putInt(42));
  }

  public void testHashTwice() {
    Hasher hasher = sha512().newHasher();

    assertThat(
            hasher
                .putString("The quick brown fox jumps over the lazy dog", UTF_8)
                .hash()
                .toString())
        .isEqualTo(
            "07e547d9586f6a73f73fbac0435ed76951218fb7d0c8d788a309d785436bbb642e93a252a954f23912547d1e8a3b5ed6e1bfd7097821233fa0538f3db854fee6");
    assertThrows(IllegalStateException.class, hasher::hash);
  }

  @SuppressWarnings("deprecation") // We still need to test our deprecated APIs.
  public void testToString() {
    assertThat(md5().toString()).isEqualTo("Hashing.md5()");
    assertThat(sha1().toString()).isEqualTo("Hashing.sha1()");
    assertThat(sha256().toString()).isEqualTo("Hashing.sha256()");
    assertThat(sha512().toString()).isEqualTo("Hashing.sha512()");
  }

  private static void assertMessageDigestHashing(byte[] input, String algorithmName)
      throws NoSuchAlgorithmException {
    MessageDigest digest = MessageDigest.getInstance(algorithmName);
    assertEquals(
        HashCode.fromBytes(digest.digest(input)), ALGORITHMS.get(algorithmName).hashBytes(input));
    for (int bytes = 4; bytes <= digest.getDigestLength(); bytes++) {
      assertEquals(
          HashCode.fromBytes(Arrays.copyOf(digest.digest(input), bytes)),
          new MessageDigestHashFunction(algorithmName, bytes, algorithmName).hashBytes(input));
    }
    int maxSize = digest.getDigestLength();
    assertThrows(
        IllegalArgumentException.class,
        () -> new MessageDigestHashFunction(algorithmName, maxSize + 1, algorithmName));
  }
}
