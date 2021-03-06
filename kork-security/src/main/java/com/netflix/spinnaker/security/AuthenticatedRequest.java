/*
 * Copyright 2015 Netflix, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.netflix.spinnaker.security;

import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;

import static java.lang.String.format;

public class AuthenticatedRequest {
  public static final String SPINNAKER_USER = "X-SPINNAKER-USER";
  public static final String SPINNAKER_ACCOUNTS = "X-SPINNAKER-ACCOUNTS";
  public static final String SPINNAKER_USER_ORIGIN = "X-SPINNAKER-USER-ORIGIN";
  public static final String SPINNAKER_REQUEST_ID = "X-SPINNAKER-REQUEST-ID";
  public static final String SPINNAKER_EXECUTION_ID = "X-SPINNAKER-EXECUTION-ID";

  public static <V> Callable<V> propagate(Callable<V> closure) {
    return propagate(closure, true, principal());
  }

  public static <V> Callable<V> propagate(Callable<V> closure, boolean restoreOriginalContext) {
    return propagate(closure, restoreOriginalContext, principal());
  }

  public static <V> Callable<V> propagate(Callable<V> closure, Object principal) {
    return propagate(closure, true, principal);
  }

  /**
   * Ensure an appropriate MDC context is available when {@code closure} is executed.
   */
  public static <V> Callable<V> propagate(Callable<V> closure, boolean restoreOriginalContext, Object principal) {
    String spinnakerUser = getSpinnakerUser(principal).orElse(null);
    String userOrigin = getSpinnakerUserOrigin().orElse(null);
    String executionId = getSpinnakerExecutionId().orElse(null);
    String requestId = getSpinnakerRequestId().orElse(null);
    String spinnakerAccounts = getSpinnakerAccounts(principal).orElse(null);

    return () -> {
      String originalSpinnakerUser = MDC.get(SPINNAKER_USER);
      String originalSpinnakerUserOrigin = MDC.get(SPINNAKER_USER_ORIGIN);
      String originalSpinnakerAccounts = MDC.get(SPINNAKER_ACCOUNTS);
      String originalSpinnakerRequestId = MDC.get(SPINNAKER_REQUEST_ID);
      String originalSpinnakerExecutionId = MDC.get(SPINNAKER_EXECUTION_ID);
      try {
        if (spinnakerUser != null) {
          MDC.put(SPINNAKER_USER, spinnakerUser);
        } else {
          MDC.remove(SPINNAKER_USER);
        }

        if (userOrigin != null) {
          MDC.put(SPINNAKER_USER_ORIGIN, userOrigin);
        } else {
          MDC.remove(SPINNAKER_USER_ORIGIN);
        }

        if (spinnakerAccounts != null) {
          MDC.put(SPINNAKER_ACCOUNTS, spinnakerAccounts);
        } else {
          MDC.remove(SPINNAKER_ACCOUNTS);
        }

        if (executionId != null) {
          MDC.put(SPINNAKER_EXECUTION_ID, executionId);
        } else {
          MDC.remove(SPINNAKER_EXECUTION_ID);
        }

        if (requestId != null) {
          MDC.put(SPINNAKER_REQUEST_ID, requestId);
        } else {
          MDC.remove(SPINNAKER_REQUEST_ID);
        }
        return closure.call();
      } finally {
        MDC.clear();

        try {
          // force clear to avoid the potential for a memory leak if log4j is being used
          Class log4jMDC = Class.forName("org.apache.log4j.MDC");
          log4jMDC.getDeclaredMethod("clear").invoke(null);
        } catch (Exception ignored) { }

        if (restoreOriginalContext) {
          if (originalSpinnakerUser != null) {
            MDC.put(SPINNAKER_USER, originalSpinnakerUser);
          }

          if (originalSpinnakerUserOrigin != null) {
            MDC.put(SPINNAKER_USER_ORIGIN, originalSpinnakerUserOrigin);
          }

          if (originalSpinnakerAccounts != null) {
            MDC.put(SPINNAKER_ACCOUNTS, originalSpinnakerAccounts);
          }

          if (originalSpinnakerRequestId != null) {
            MDC.put(SPINNAKER_REQUEST_ID, originalSpinnakerRequestId);
          }

          if (originalSpinnakerExecutionId != null) {
            MDC.put(SPINNAKER_EXECUTION_ID, originalSpinnakerExecutionId);
          }
        }
      }
    };
  }

  public static Map<String, Optional<String>> getAuthenticationHeaders() {
    Map<String, Optional<String>> headers = new HashMap<>();
    headers.put(SPINNAKER_USER, getSpinnakerUser());
    headers.put(SPINNAKER_ACCOUNTS, getSpinnakerAccounts());
    headers.put(SPINNAKER_USER_ORIGIN, getSpinnakerUserOrigin());
    headers.put(SPINNAKER_REQUEST_ID, getSpinnakerRequestId());
    headers.put(SPINNAKER_EXECUTION_ID, getSpinnakerExecutionId());
    return headers;
  }

  public static Optional<String> getSpinnakerUser() {
    return getSpinnakerUser(principal());
  }

  public static Optional<String> getSpinnakerUser(Object principal) {
    Object spinnakerUser = MDC.get(SPINNAKER_USER);

    if (principal != null && principal instanceof User) {
      spinnakerUser = ((User) principal).getUsername();
    }

    return Optional.ofNullable((String) spinnakerUser);
  }

  public static Optional<String> getSpinnakerAccounts() {
    return getSpinnakerAccounts(principal());
  }

  public static Optional<String> getSpinnakerAccounts(Object principal) {
    Object spinnakerAccounts = MDC.get(SPINNAKER_ACCOUNTS);

    if (principal instanceof User && !CollectionUtils.isEmpty(((User) principal).allowedAccounts)) {
      spinnakerAccounts = String.join(",", ((User) principal).getAllowedAccounts());
    }

    return Optional.ofNullable((String) spinnakerAccounts);
  }

  public static Optional<String> getSpinnakerUserOrigin() {
    return Optional.ofNullable(MDC.get(SPINNAKER_USER_ORIGIN));
  }

  /**
   * Returns or creates a spinnaker request ID.
   *
   * If a request ID already exists, it will be propagated without change.
   * If a request ID does not already exist:
   *
   * 1. If an execution ID exists, it will create a hierarchical request ID
   *    using the execution ID, followed by a UUID.
   * 2. If an execution ID does not exist, it will create a simple UUID request id.
   */
  public static Optional<String> getSpinnakerRequestId() {
    return Optional.of(
      Optional
        .ofNullable(MDC.get(SPINNAKER_REQUEST_ID))
        .orElse(
          getSpinnakerExecutionId()
            .map(id -> format("%s:%s", id, UUID.randomUUID().toString()))
            .orElse(UUID.randomUUID().toString())
        )
    );
  }

  public static Optional<String> getSpinnakerExecutionId() {
    return Optional.ofNullable(MDC.get(SPINNAKER_EXECUTION_ID));
  }

  /**
   * @return the Spring Security principal or null if there is no authority.
   */
  private static Object principal() {
    return Optional
      .ofNullable(SecurityContextHolder.getContext().getAuthentication())
      .map(Authentication::getPrincipal)
      .orElse(null);
  }
}
