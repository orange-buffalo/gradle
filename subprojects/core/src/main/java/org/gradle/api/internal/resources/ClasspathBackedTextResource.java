/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.internal.resources;


import com.google.common.io.CharSource;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;

public class ClasspathBackedTextResource extends CharSourceBackedTextResource {

    private final Class<?> baseClass;
    private final String path;
    private final Charset charset;

    public ClasspathBackedTextResource(Class<?> baseClass, String path, Charset charset) {
        super("Classpath resource '" + path + "'", new CharSource() {
            @Override
            public Reader openStream() {
                InputStream stream = baseClass.getResourceAsStream(path);
                if (stream == null) {
                    throw new IllegalStateException("Could not find class path resource " + path + " relative to " + baseClass.getName());
                }
                return new BufferedReader(new InputStreamReader(stream, charset));
            }
        });
        this.baseClass = baseClass;
        this.path = path;
        this.charset = charset;
    }

    public Class<?> getBaseClass() {
        return baseClass;
    }

    public String getPath() {
        return path;
    }

    public Charset getCharset() {
        return charset;
    }
}
