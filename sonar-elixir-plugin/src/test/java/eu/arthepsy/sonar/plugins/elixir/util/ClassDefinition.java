/*
 * SonarQube Elixir plugin
 * Copyright (C) 2015 Andris Raugulis
 * moo@arthepsy.eu
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package eu.arthepsy.sonar.plugins.elixir.util;

import static org.junit.Assert.fail;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.fest.assertions.Assertions.assertThat;

public final class ClassDefinition {

    public static void testFinalClassDefinition(Class<?> clazz) {
        testFinalClassDefinition(clazz, false);
    }

    public static void testFinalClassDefinition(Class<?> clazz, Boolean privateConstructor) {
        assertThat(Modifier.isFinal(clazz.getModifiers())).isTrue();
        final Constructor<?> constructor;
        try {
            constructor = clazz.getDeclaredConstructor();
        } catch (NoSuchMethodException e) {
            fail(e.getMessage());
            return;
        }
        if (privateConstructor) {
            assertThat(Modifier.isPrivate(constructor.getModifiers())).isTrue();
        }
        constructor.setAccessible(true);
        try {
            constructor.newInstance();
        } catch (InstantiationException e) {
            fail(e.getMessage());
            return;
        } catch (IllegalAccessException e) {
            fail(e.getMessage());
            return;
        } catch (InvocationTargetException e) {
            fail(e.getMessage());
            return;
        }
        constructor.setAccessible(false);
        for (final Method method: clazz.getMethods()) {
            if (method.getDeclaringClass().equals(clazz)) {
                assertThat(Modifier.isStatic(method.getModifiers())).isTrue();
            }
        }
    }

}
