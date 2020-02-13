/*
 * Copyright 2010 the original author or authors.
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

package org.gradle.api;

import org.gradle.api.file.FileSystemOperations;
import org.gradle.api.file.ProjectLayout;
import org.gradle.api.internal.AbstractTask;
import org.gradle.api.model.ObjectFactory;
import org.gradle.internal.extensibility.NoConventionMapping;

import javax.inject.Inject;

/**
 * {@code DefaultTask} is the standard {@link Task} implementation. You can extend this to implement your own task types.
 */
@NoConventionMapping
public class DefaultTask extends AbstractTask {

    /**
     * Provides access to methods to create various kinds of model objects.
     *
     * @return the object factory
     * @since 6.3
     */
    @Inject
    @Incubating
    protected ObjectFactory getObjectFactory() {
        throw new UnsupportedOperationException();
    }

    /**
     * Provides access to various important directories for this project.
     *
     * @return the project layout
     * @since 6.3
     */
    @Inject
    @Incubating
    protected ProjectLayout getProjectLayout() {
        throw new UnsupportedOperationException();
    }

    /**
     * Provides access to various file system operations.
     *
     * @return the file system operations
     * @since 6.3
     */
    @Inject
    @Incubating
    protected FileSystemOperations getFileSystemOperations() {
        throw new UnsupportedOperationException();
    }
}
