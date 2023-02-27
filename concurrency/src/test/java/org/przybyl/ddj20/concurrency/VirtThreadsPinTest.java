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

import com.sun.net.httpserver.*;
import org.junit.jupiter.api.*;
import org.moditect.jfrunit.*;
import org.przybyl.ddj20.concurrency.virtThreads.*;

import java.net.*;
import java.net.http.*;
import java.nio.charset.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

@JfrEventTest
public class VirtThreadsPinTest {

    private final Random random = new Random();
    public JfrEvents jfrEvents = new JfrEvents();
    private HttpServer server;

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress(8080), 1_000_000);
        server.createContext("/", exchange -> {
            int sum = random.ints(15_000_000).sum();
            var responseBytes = (sum + "").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("content-type", "text/plain");
            exchange.sendResponseHeaders(200, responseBytes.length);
            exchange.getResponseBody().write(responseBytes);
            exchange.close();
        });
        server.setExecutor(Executors.newSingleThreadExecutor());
        server.start();
    }

    @AfterEach
    void tearDown() {
        server.stop(1);
    }

    @Test
    @EnableEvent("jdk.VirtualThreadPinned")
    public void shouldNotPin() throws InterruptedException {

        var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        var request = HttpRequest.newBuilder(URI.create("http://localhost:8080")).GET().build();

        VirtThreads.getGreetings(client, request, 20);

        jfrEvents.awaitEvents();
        Assertions.assertTrue(jfrEvents.events().findAny().isEmpty(), "there should be no pinned events");
    }

}
