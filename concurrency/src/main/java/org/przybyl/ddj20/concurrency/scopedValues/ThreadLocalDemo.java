/*
 *  Copyright (C) 2023 Piotr Przybył
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
package org.przybyl.ddj20.concurrency.scopedValues;


import java.util.concurrent.atomic.AtomicInteger;


public class ThreadLocalDemo {

    static final ThreadLocal<AtomicInteger> tlCalls = new InheritableThreadLocal<>();


    public static void main(String[] args) throws InterruptedException {

        tlCalls.set(new AtomicInteger(0));
        tlCalls.get().getAndIncrement();
        System.out.println("Calls so far [0] " + tlCalls.get().get());

        var subThread = new Thread(() -> {
            tlCalls.get().getAndIncrement();
//            tlCalls.set(new AtomicInteger(-42));
            System.out.println("Calls so far [1] " + tlCalls.get().get());
        });

        subThread.start();
        subThread.join();
        tlCalls.get().getAndIncrement();
        System.out.println("Calls so far [2] " + tlCalls.get().get());


    }

}
