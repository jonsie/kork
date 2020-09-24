/*
 * Copyright 2017 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.kork.web.exceptions

import com.netflix.spinnaker.kork.api.exceptions.ExceptionDetails
import com.netflix.spinnaker.kork.api.exceptions.ExceptionMessage
import org.springframework.beans.BeansException
import org.springframework.beans.factory.ObjectProvider
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

import javax.annotation.Nullable
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse;

class GenericExceptionHandlersSpec extends Specification {
  @Shared
  String messageToBeAppended = "Message to be appended."

  ExceptionMessageDecorator userMessageService = new ExceptionMessageDecorator(
    new UserMessageAppenderProvider([new AccessDeniedExceptionMessage(messageToBeAppended)])
  )

  @Subject
  def genericExceptionHandlers = new GenericExceptionHandlers(userMessageService)

  def request = Mock(HttpServletRequest)
  def response = Mock(HttpServletResponse)

  @Unroll
  def "should favor exception message when present (otherwise use ResponseStatus.reason())"() {
    when:
    genericExceptionHandlers.handleException(exception, response, request)

    then:
    1 * response.sendError(expectedStatusCode, expectedReason)

    where:
    exception                                || expectedStatusCode || expectedReason
    new E1()                                 || 404                || "Default Reason!"
    new E1("")                               || 404                || "Default Reason!"
    new E1("  ")                             || 404                || "Default Reason!"
    new E1("E1 Reason")                      || 404                || "E1 Reason"
    new E2()                                 || 404                || "Default Reason!"
    new E2("E2 Reason")                      || 404                || "E2 Reason"
    new E3()                                 || 404                || "Default Reason!"
    new E4()                                 || 400                || "My Other Reason!" // favor @ResponseStatus on interface over super class
    new E5("E5 Reason")                      || 400                || "E5 Reason"
    new NullPointerException("It's an NPE!") || 500                || "It's an NPE!"
    new AccessDenied()                       || 403                || "Access is denied" + "\n" + messageToBeAppended
  }

  @ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "Default Reason!")
  class E1 extends RuntimeException {
    E1() {
      super()
    }

    E1(String message) {
      super(message)
    }
  }

  class E2 extends E1 {
    E2() {
      super()
    }

    E2(String message) {
      super(message)
    }
  }

  class E3 extends E2 {
    E3() {
      super()
    }

    E3(String message) {
      super(message)
    }
  }

  @ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "My Other Reason!")
  interface I1 {

  }

  class E4 extends E3 implements I1 {
    E4() {
      super()
    }

    E4(String message) {
      super(message)
    }
  }

  class E5 extends E4 {
    E5() {
      super()
    }

    E5(String message) {
      super(message)
    }
  }

  @ResponseStatus(value = HttpStatus.FORBIDDEN, reason = "Access is denied")
  class AccessDenied extends E1 {
    AccessDenied() {
      super()
    }

    AccessDenied(String message) {
      super(message)
    }
  }

}

class UserMessageAppenderProvider implements ObjectProvider<List<ExceptionMessage>> {

  List<ExceptionMessage> userMessageAppenders

  UserMessageAppenderProvider(List<ExceptionMessage> userMessageAppenders) {
    this.userMessageAppenders = userMessageAppenders
  }

  @Override
  List<ExceptionMessage> getObject(Object... args) throws BeansException {
    return userMessageAppenders
  }

  @Override
  List<ExceptionMessage> getIfAvailable() throws BeansException {
    return userMessageAppenders
  }

  @Override
  List<ExceptionMessage> getIfUnique() throws BeansException {
    return userMessageAppenders
  }

  @Override
  List<ExceptionMessage> getObject() throws BeansException {
    return userMessageAppenders
  }
}

class AccessDeniedExceptionMessage implements ExceptionMessage {

  private String messageToBeAppended

  AccessDeniedExceptionMessage(String messageToBeAppended) {
    this.messageToBeAppended = messageToBeAppended
  }

  @Override
  boolean supports(Class<? extends Throwable> throwable) {
    return throwable == GenericExceptionHandlersSpec.AccessDenied.class
  }

  @Override
  String message(Throwable throwable, @Nullable ExceptionDetails exceptionDetails) {
    return messageToBeAppended
  }
}
