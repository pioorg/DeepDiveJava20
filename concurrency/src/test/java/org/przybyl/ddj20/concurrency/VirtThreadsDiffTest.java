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

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;

class VirtThreadsDiffTest {

    Thread thread = Thread.ofVirtual().unstarted(() -> {});

    @Test
    void shouldThrowExceptionsForUnsupportedMethods() {
        assertThrows(UnsupportedOperationException.class, () -> thread.stop());
        assertThrows(UnsupportedOperationException.class, () -> thread.suspend());
        assertThrows(UnsupportedOperationException.class, () -> thread.resume());
        assertThrows(IllegalArgumentException.class, () -> thread.setDaemon(false));
    }

    @Test
    void shouldOnlyBeDaemon() {
        thread.setDaemon(true);
        assertTrue(thread.isDaemon());
    }

    @Test
    void shouldAlwaysHaveNormalPriority() {
        thread.setPriority(Thread.MAX_PRIORITY);
        assertEquals(Thread.NORM_PRIORITY, thread.getPriority());
    }

    // everything below might not be relevant for JDK 21, it seems

    @Test
    void shouldSuportThreadLocalVariablesByDefault() throws InterruptedException {
        var tl = new ThreadLocal<Integer>();
        Thread.startVirtualThread(() -> {
            assertNull(tl.get());
            tl.set(42);
            assertEquals(42, tl.get());
        }).join();
    }

    @Test
    void shouldAllowOptOutFromThreadLocal() throws InterruptedException {
        var tl = new ThreadLocal<Integer>();
        Thread.ofVirtual().allowSetThreadLocals(false).start(() -> {
            assertThrows(UnsupportedOperationException.class, () -> {
                tl.set(17);
            });
        }).join();
    }

    @Test
    void shouldSupportInheritableThreadLocal() throws InterruptedException {
        var itl = new InheritableThreadLocal<Integer>();
        Thread.startVirtualThread(() -> {
            itl.set(33);
            Thread.startVirtualThread(() -> {
                assertEquals(33, itl.get());
            });
        }).join();
    }

    @Test
    void shouldSupportOptingOutInheritableThreadLocal() throws InterruptedException {
        var itl = new InheritableThreadLocal<Integer>();
        Thread.startVirtualThread(() -> {
            itl.set(33);
            Thread.ofVirtual().inheritInheritableThreadLocals(false).start(() -> {
                assertNull(itl.get());
            });
        }).join();
    }

}