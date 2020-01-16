/*
 * Copyright 2020 Netflix, Inc.
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

package com.netflix.spinnaker.kork.plugins.update.front50

import com.netflix.spinnaker.kork.exceptions.SystemException
import com.netflix.spinnaker.kork.plugins.update.SpinnakerPluginInfo
import dev.minutest.junit.JUnit5Minutests
import dev.minutest.rootContext
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.assertThrows
import org.pf4j.update.SimpleFileDownloader
import org.pf4j.update.verifier.CompoundVerifier
import retrofit2.HttpException
import strikt.api.expectThat
import strikt.assertions.get
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import java.net.URL
import java.util.Collections

class Front50UpdateRepositoryTest : JUnit5Minutests {
  fun tests() = rootContext<Fixture> {
    fixture { Fixture() }

    test("getPlugins populates the plugins cache, subsequent getPlugin returns cached item") {
      every { front50Service.list(applicationName) } returns Collections.singletonList(plugin)

      val plugins = subject.getPlugins()

      expectThat(plugins)
        .isA<MutableMap<String, SpinnakerPluginInfo>>()[pluginId]
        .get { plugin.id }.isEqualTo(pluginId)

      val plugin = subject.getPlugin(pluginId)

      expectThat(plugin)
        .isA<SpinnakerPluginInfo>()
    }

    test("HttpException results in thrown SystemException") {
      every { front50Service.list(applicationName) } throws HttpException(mockk(relaxed = true))
      assertThrows<SystemException> { (subject.getPlugins()) }
    }

    test("Returns repository ID and URL") {
      expectThat(subject.id).isEqualTo(repositoryName)
      expectThat(subject.url).isEqualTo(front50Url)
    }
  }

  private inner class Fixture {
    val front50Service: Front50Service = mockk(relaxed = true)
    val pluginId = "netflix.custom-stage"
    val applicationName = "orca"
    val repositoryName = "front50"
    val front50Url = URL("https://front50.com")

    val subject = Front50UpdateRepository(
      repositoryName,
      applicationName,
      front50Url,
      SimpleFileDownloader(),
      CompoundVerifier(),
      front50Service
    )

    val plugin = SpinnakerPluginInfo(SpinnakerPluginInfo.State.RELEASE)

    init {
      plugin.id = pluginId
    }
  }
}