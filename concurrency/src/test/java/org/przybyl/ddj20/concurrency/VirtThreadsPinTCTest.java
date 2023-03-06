/*
 *  Copyright (C) 2022 Piotr Przybył
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.przybyl.ddj20.concurrency;

import eu.rekawek.toxiproxy.model.*;
import static org.hamcrest.CoreMatchers.*;
import org.hamcrest.*;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.*;
import org.testcontainers.containers.wait.strategy.*;
import org.testcontainers.utility.*;

import java.io.*;
import java.nio.file.*;
import java.util.stream.*;

class VirtThreadsPinTCTest {

    @Test
    @Disabled
    public void shouldNotPin() throws IOException {

        // we're going to copy this file from resources to nginx container
        var index = MountableFile.forClasspathResource("index.html");

        try (
            var nginx = new NginxContainer<>("nginx:1.23.1")
                .withCopyFileToContainer(index, "/usr/share/nginx/html/index.html")
                .waitingFor(new HttpWaitStrategy());
            var toxiproxy = new ToxiproxyContainer("ghcr.io/shopify/toxiproxy:2.5.0")
                .withNetworkAliases("toxiproxy")
        ) {
            // starting both containers in parallel
            Stream.of(nginx, toxiproxy).parallel().forEach(GenericContainer::start);

            // creating intoxicated connection to be used between our client and nginx
            var proxy = toxiproxy.getProxy(nginx, 80);
            proxy.toxics().latency("latency", ToxicDirection.DOWNSTREAM, 500).setJitter(50);

            // preparing the artifact to be copied
            var jar = MountableFile.forHostPath(Paths.get("target/concurrency-1.0-SNAPSHOT.jar"));

            try (var container = new GenericContainer<>("openjdk:20-slim")
                .withCopyFileToContainer(jar, "/tmp/test.jar")
                .withExposedPorts(8000)
                .withCommand("jwebserver")) {

                // starting container for the client with the client already copied
                container.start();

                // where the client should call
                var uriFromContainer = String.format("http://%s:%d/", "toxiproxy", proxy.getOriginalProxyPort());

                Assertions.assertDoesNotThrow(() -> {

                    // running the client which should call the nginx using intoxicated proxy
                    var result = container.execInContainer(
                        "java", "--enable-preview", "-Djdk.tracePinnedThreads=full", "-jar", "/tmp/test.jar", uriFromContainer);

                    // eventually it should exit successfully
                    Assertions.assertEquals(0, result.getExitCode());

                    // and there should be no virtual threads pinned
                    MatcherAssert.assertThat(result.getStdout(), not(containsString("onPinned")));
                });
            }
        }
    }

}