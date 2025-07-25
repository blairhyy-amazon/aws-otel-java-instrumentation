/*
 * Copyright Amazon.com, Inc. or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.opentelemetry.javaagent.providers;

import java.util.Optional;

public class SqsUrlParser {
  private static final String HTTP_SCHEMA = "http://";
  private static final String HTTPS_SCHEMA = "https://";

  /**
   * Best-effort logic to extract queue name from an HTTP url. This method should only be used with
   * a string that is, with reasonably high confidence, an SQS queue URL. Handles new/legacy/some
   * custom URLs. Essentially, we require that the URL should have exactly three parts, delimited by
   * /'s (excluding schema), the second part should be a 12-digit account id, and the third part
   * should be a valid queue name, per SQS naming conventions.
   */
  public static Optional<String> getQueueName(String url) {
    if (url == null) {
      return Optional.empty();
    }
    String urlWithoutProtocol = url.replace(HTTP_SCHEMA, "").replace(HTTPS_SCHEMA, "");
    String[] splitUrl = urlWithoutProtocol.split("/");
    if (splitUrl.length == 3 && isAccountId(splitUrl[1]) && isValidQueueName(splitUrl[2])) {
      return Optional.of(splitUrl[2]);
    }
    return Optional.empty();
  }

  /** Extracts the account ID from an SQS URL. */
  public static Optional<String> getAccountId(String url) {
    ParsedUrl parsed = parseUrl(url);
    return Optional.ofNullable(parsed.accountId);
  }

  /** Extracts the region from an SQS URL. */
  public static Optional<String> getRegion(String url) {
    ParsedUrl parsed = parseUrl(url);
    return Optional.ofNullable(parsed.region);
  }

  /**
   * Parses an SQS URL and extracts its components. URL Format:
   * https://sqs.<region>.amazonaws.com/<accountId>/<queueName>
   */
  private static ParsedUrl parseUrl(String url) {
    if (url == null) {
      return new ParsedUrl(null, null, null);
    }

    String urlWithoutProtocol = url.replace(HTTP_SCHEMA, "").replace(HTTPS_SCHEMA, "");
    String[] splitUrl = urlWithoutProtocol.split("/");

    if (splitUrl.length != 3
        || !isAccountId(splitUrl[1])
        || !isValidQueueName(splitUrl[2])
        || !splitUrl[0].toLowerCase().startsWith("sqs")) {
      return new ParsedUrl(null, null, null);
    }

    String domain = splitUrl[0];
    String[] domainParts = domain.split("\\.");

    String region = domainParts.length == 4 ? domainParts[1] : null;
    return new ParsedUrl(splitUrl[2], splitUrl[1], region);
  }

  private static boolean isAccountId(String input) {
    if (input == null) {
      return false;
    }

    try {
      Long.valueOf(input);
    } catch (Exception e) {
      return false;
    }

    return true;
  }

  private static boolean isValidQueueName(String input) {
    if (input == null || input.length() == 0 || input.length() > 80) {
      return false;
    }

    for (Character c : input.toCharArray()) {
      if (c != '_' && c != '-' && !Character.isAlphabetic(c) && !Character.isDigit(c)) {
        return false;
      }
    }

    return true;
  }

  private static class ParsedUrl {
    private final String queueName;
    private final String accountId;
    private final String region;

    private ParsedUrl(String queueName, String accountId, String region) {
      this.queueName = queueName;
      this.accountId = accountId;
      this.region = region;
    }
  }
}
