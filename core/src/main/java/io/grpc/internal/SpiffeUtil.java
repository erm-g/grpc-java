/*
 * Copyright 2024 The gRPC Authors
 *
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

package io.grpc.internal;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Splitter;
import java.util.Locale;

/**
 * Helper utility to work with SPIFFE URIs.
 * @see <a href="https://github.com/spiffe/spiffe/blob/master/standards/SPIFFE-ID.md">Standard</a>
 */
public final class SpiffeUtil {

  private static final String PREFIX = "spiffe://";

  private SpiffeUtil() {}

  /**
   * Parses a URI string, applies validation rules described in SPIFFE standard, and, in case of
   * success, returns parsed TrustDomain and Path.
   *
   * @param uri a String representing a SPIFFE ID
   */
  public static SpiffeId parse(String uri) {
    doInitialUriValidation(uri);
    checkArgument(uri.toLowerCase(Locale.US).startsWith(PREFIX), "Spiffe Id must start with "
        + PREFIX);
    String domainAndPath = uri.substring(PREFIX.length());
    String trustDomain;
    String path;
    if (!domainAndPath.contains("/")) {
      trustDomain = domainAndPath;
      path =  "";
    } else {
      String[] parts = domainAndPath.split("/", 2);
      trustDomain = parts[0];
      path = parts[1];
      checkArgument(!path.isEmpty(), "Path must not include a trailing '/'");
    }
    validateTrustDomain(trustDomain);
    validatePath(path);
    if (!path.isEmpty()) {
      path = "/" + path;
    }
    return new SpiffeId(trustDomain, path);
  }

  private static void doInitialUriValidation(String uri) {
    checkArgument(checkNotNull(uri, "uri").length() > 0, "Spiffe Id can't be empty");
    checkArgument(uri.length() <= 2048, "Spiffe Id maximum length is 2048 characters");
    checkArgument(!uri.contains("#"), "Spiffe Id must not contain query fragments");
    checkArgument(!uri.contains("?"), "Spiffe Id must not contain query parameters");
  }

  private static void validateTrustDomain(String trustDomain) {
    checkArgument(!trustDomain.isEmpty(), "Trust Domain can't be empty");
    checkArgument(trustDomain.length() < 256, "Trust Domain maximum length is 255 characters");
    checkArgument(trustDomain.matches("[a-z0-9._-]+"),
        "Trust Domain must contain only letters, numbers, dots, dashes, and underscores"
            + " ([a-z0-9.-_])");
  }

  private static void validatePath(String path) {
    if (path.isEmpty()) {
      return;
    }
    checkArgument(!path.endsWith("/"), "Path must not include a trailing '/'");
    for (String segment : Splitter.on("/").split(path)) {
      validatePathSegment(segment);
    }
  }

  private static void validatePathSegment(String pathSegment) {
    checkArgument(!pathSegment.isEmpty(), "Individual path segments must not be empty");
    checkArgument(!(pathSegment.equals(".") || pathSegment.equals("..")),
        "Individual path segments must not be relative path modifiers (i.e. ., ..)");
    checkArgument(pathSegment.matches("[a-zA-Z0-9._-]+"),
        "Individual path segments must contain only letters, numbers, dots, dashes, and underscores"
            + " ([a-zA-Z0-9.-_])");
  }

  /**
   * Represents a SPIFFE ID as defined in the SPIFFE standard.
   * @see <a href="https://github.com/spiffe/spiffe/blob/master/standards/SPIFFE-ID.md">Standard</a>
   */
  public static class SpiffeId {

    private final String trustDomain;
    private final String path;

    private SpiffeId(String trustDomain, String path) {
      this.trustDomain = trustDomain;
      this.path = path;
    }

    public String getTrustDomain() {
      return trustDomain;
    }

    public String getPath() {
      return path;
    }
  }

}
