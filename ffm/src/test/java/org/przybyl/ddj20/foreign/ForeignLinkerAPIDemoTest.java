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

package org.przybyl.ddj20.foreign;

import org.junit.jupiter.api.*;
import org.testcontainers.containers.*;
import org.testcontainers.utility.*;

import java.nio.file.*;

class ForeignLinkerAPIDemoTest {

    @Test
    @Disabled
    public void shouldNotWorkForRoot() {
        var jar = MountableFile.forHostPath(Paths.get("target/ffm-1.0-SNAPSHOT.jar"));

        try (var container = new GenericContainer<>("openjdk:20-slim")
            .withCopyFileToContainer(jar, "/tmp/test.jar")
            .withExposedPorts(8000)
            .withCommand("jwebserver")) {

            container.start();

            Assertions.assertDoesNotThrow(() -> {
                var result = container.execInContainer("java", "--enable-preview", "-jar", "/tmp/test.jar");
                Assertions.assertNotEquals(0, result.getExitCode());
                Assertions.assertTrue(result.getStderr().contains("Oh no, don't run me as root"));
            });
        }
    }
}