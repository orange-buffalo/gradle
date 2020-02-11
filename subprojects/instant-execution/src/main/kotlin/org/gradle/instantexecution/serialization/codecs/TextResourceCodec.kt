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

package org.gradle.instantexecution.serialization.codecs

import org.gradle.api.internal.file.FileOperations
import org.gradle.api.internal.resources.ApiTextResourceAdapter
import org.gradle.api.internal.resources.ClasspathBackedTextResource
import org.gradle.api.internal.resources.FileCollectionBackedArchiveTextResource
import org.gradle.api.internal.resources.FileCollectionBackedTextResource
import org.gradle.api.resources.TextResource
import org.gradle.api.resources.TextResourceFactory
import org.gradle.instantexecution.serialization.Codec
import org.gradle.instantexecution.serialization.ReadContext
import org.gradle.instantexecution.serialization.WriteContext
import org.gradle.instantexecution.serialization.ownerService
import org.gradle.instantexecution.serialization.readFile
import org.gradle.instantexecution.serialization.writeFile
import java.nio.charset.Charset


object TextResourceCodec : Codec<TextResource> {

    override suspend fun WriteContext.encode(value: TextResource) {
        when (value) {
            is ApiTextResourceAdapter -> {
                writeSmallInt(API_ADAPTER)
                writeString(value.uri.toASCIIString())
            }
            is FileCollectionBackedArchiveTextResource -> {
                writeSmallInt(ARCHIVE)
                writeFile(value.archiveFile)
                writeString(value.path)
                writeString(value.charset.name())
            }
            is FileCollectionBackedTextResource -> {
                writeSmallInt(FILE_COLLECTION)
                writeFile(value.inputFiles!!.singleFile)
                writeString(value.charset.name())
            }
            is ClasspathBackedTextResource -> {
                writeSmallInt(CLASSPATH)
                writeClass(value.baseClass)
                writeString(value.path)
                writeString(value.charset.name())
            }
            else -> {
                writeSmallInt(STRING)
                writeString(value.asString())
            }
        }
    }

    override suspend fun ReadContext.decode(): TextResource =
        when (readSmallInt()) {
            API_ADAPTER -> textResourceFactory.fromInsecureUri(readString())
            ARCHIVE -> textResourceFactory.fromArchiveEntry(readFile(), readString(), readString())
            FILE_COLLECTION -> textResourceFactory.fromFile(readFile(), readString())
            CLASSPATH -> ClasspathBackedTextResource(readClass(), readString(), Charset.forName(readString()))
            else -> textResourceFactory.fromString(readString())
        }

    private
    val ReadContext.textResourceFactory: TextResourceFactory
        get() = ownerService<FileOperations>().resources.text
}


private
const val STRING = 0


private
const val API_ADAPTER = 1


private
const val ARCHIVE = 2


private
const val FILE_COLLECTION = 3


private
const val CLASSPATH = 4
