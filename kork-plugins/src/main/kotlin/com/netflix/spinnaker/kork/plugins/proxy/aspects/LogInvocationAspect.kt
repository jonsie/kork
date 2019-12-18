/*
 * Copyright 2019 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netflix.spinnaker.kork.plugins.proxy.aspects

import com.netflix.spinnaker.kork.plugins.SpinnakerPluginDescriptor
import org.slf4j.LoggerFactory
import java.lang.reflect.Method

class LogInvocationAspect : InvocationAspect<LogMethodInvocationState> {
  private val log by lazy { LoggerFactory.getLogger(javaClass) }

  override fun supports(methodInvocationState: Class<MethodInvocationState>): Boolean {
    TODO("not implemented")
  }

  override fun create(
    target: Any,
    proxy: Any,
    method: Method,
    args: Array<out Any>?,
    descriptor: SpinnakerPluginDescriptor
  ): LogMethodInvocationState {
    TODO("not implemented")
  }

  override fun after(success: Boolean, methodInvocationState: LogMethodInvocationState) {
    TODO("not implemented")
  }
}
