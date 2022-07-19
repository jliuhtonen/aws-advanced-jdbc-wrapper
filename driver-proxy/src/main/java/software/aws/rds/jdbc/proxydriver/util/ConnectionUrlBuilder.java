/*
 * AWS JDBC Proxy Driver
 * Copyright Amazon.com Inc. or affiliates.
 * See the LICENSE file in the project root for more information.
 */

package software.aws.rds.jdbc.proxydriver.util;

import static software.aws.rds.jdbc.proxydriver.ConnectionPropertyNames.DATABASE_PROPERTY_NAME;
import static software.aws.rds.jdbc.proxydriver.ConnectionPropertyNames.PASSWORD_PROPERTY_NAME;
import static software.aws.rds.jdbc.proxydriver.ConnectionPropertyNames.USER_PROPERTY_NAME;
import static software.aws.rds.jdbc.proxydriver.util.StringUtils.isNullOrEmpty;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Properties;
import software.aws.rds.jdbc.proxydriver.HostSpec;

public class ConnectionUrlBuilder {
  // Builds a connection URL of the generic format: "protocol//[hosts][/database][?properties]"
  public static String buildUrl(String jdbcProtocol,
                                HostSpec hostSpec,
                                String serverPropertyName,
                                String portPropertyName,
                                String databasePropertyName,
                                String userPropertyName,
                                String passwordPropertyName,
                                Properties props) throws SQLException {
    if (isNullOrEmpty(jdbcProtocol)
        || ((isNullOrEmpty(serverPropertyName) || isNullOrEmpty(props.getProperty(serverPropertyName)))
            && hostSpec == null)) {
      throw new SQLException("Missing JDBC protocol and/or host name. Could not construct URL.");
    }

    final Properties copy = PropertyUtils.copyProperties(props);
    final StringBuilder urlBuilder = new StringBuilder();
    urlBuilder.append(jdbcProtocol);

    if (!jdbcProtocol.contains("//")) {
      urlBuilder.append("//");
    }

    if (hostSpec != null) {
      urlBuilder.append(hostSpec.getUrl());
    } else {
      urlBuilder.append(copy.get(serverPropertyName));

      if (!isNullOrEmpty(portPropertyName) && !isNullOrEmpty(copy.getProperty(portPropertyName))) {
        urlBuilder.append(":").append(copy.get(portPropertyName));
      }

      urlBuilder.append("/");
    }

    if (!isNullOrEmpty(copy.getProperty(DATABASE_PROPERTY_NAME))) {
      urlBuilder.append(copy.get(DATABASE_PROPERTY_NAME));
      copy.remove(DATABASE_PROPERTY_NAME);
    }

    removeProperty(serverPropertyName, copy);
    removeProperty(portPropertyName, copy);
    removeProperty(databasePropertyName, copy);
    removeProperty(userPropertyName, copy);
    removeProperty(passwordPropertyName, copy);

    final StringBuilder queryBuilder = new StringBuilder();
    final Enumeration<?> propertyNames = copy.propertyNames();
    while (propertyNames.hasMoreElements()) {
      String propertyName = propertyNames.nextElement().toString();
      if (queryBuilder.length() != 0) {
        queryBuilder.append("&");
      }

      final String propertyValue = copy.getProperty(propertyName);
      if (propertyName.equals(USER_PROPERTY_NAME) && !isNullOrEmpty(userPropertyName)) {
        propertyName = userPropertyName;
      } else if (propertyName.equals(PASSWORD_PROPERTY_NAME) && !isNullOrEmpty(passwordPropertyName)) {
        propertyName = passwordPropertyName;
      }

      try {
        queryBuilder
            .append(propertyName)
            .append("=")
            .append(URLEncoder.encode(propertyValue, StandardCharsets.UTF_8.toString()));
      } catch (UnsupportedEncodingException e) {
        throw new SQLException("Was not able to encode connectionURL properties.", e);
      }
    }

    if (queryBuilder.length() != 0) {
      urlBuilder.append("?").append(queryBuilder);
    }

    return urlBuilder.toString();
  }

  private static void removeProperty(String propertyKey, Properties props) {
    if (!isNullOrEmpty(propertyKey) && !isNullOrEmpty(props.getProperty(propertyKey))) {
      props.remove(propertyKey);
    }
  }
}
