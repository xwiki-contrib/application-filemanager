/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.filemanager.internal;

import java.io.File;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.resource.temporary.TemporaryResourceReference;
import org.xwiki.resource.temporary.TemporaryResourceStore;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PackFileResolver}.
 *
 * @version $Id$
 * @since 2.1
 */
@ComponentTest
class PackFileResolverTest
{
    private static final AttachmentReference OUTPUT_FILE_REFERENCE =
        new AttachmentReference("out.zip", new DocumentReference("wiki", "Space", "Page"));

    @InjectMockComponents
    private PackFileResolver packFileResolver;

    @MockComponent
    private TemporaryResourceStore temporaryResourceStore;

    @TempDir
    private File tempDir;

    @Test
    void getTemporaryResourceReference()
    {
        TemporaryResourceReference reference =
            this.packFileResolver.getTemporaryResourceReference(OUTPUT_FILE_REFERENCE);

        assertEquals("filemanager", reference.getModuleId());
        assertEquals(Collections.singletonList("out.zip"), reference.getResourcePath());
        assertEquals(OUTPUT_FILE_REFERENCE.getDocumentReference(), reference.getOwningEntityReference());
    }

    @Test
    void getTemporaryFile() throws Exception
    {
        File expectedFile = new File(new File(this.tempDir, "filemanager/a/b/cdef"), "out.zip");
        when(this.temporaryResourceStore
            .getTemporaryFile(this.packFileResolver.getTemporaryResourceReference(OUTPUT_FILE_REFERENCE)))
                .thenReturn(expectedFile);

        File file = this.packFileResolver.getTemporaryFile(OUTPUT_FILE_REFERENCE);

        assertEquals(expectedFile, file);
        assertTrue(file.getParentFile().isDirectory(), "The parent folders should have been created");
    }
}
