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
package org.xwiki.filemanager.internal.job;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.xwiki.filemanager.File;
import org.xwiki.filemanager.Folder;
import org.xwiki.filemanager.Path;
import org.xwiki.filemanager.job.BatchPathRequest;
import org.xwiki.job.Job;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DeleteJob}.
 * 
 * @version $Id$
 * @since 2.0M1
 */
@ComponentTest
class DeleteJobTest extends AbstractJobTest
{
    @InjectMockComponents
    private DeleteJob deleteJob;

    @Override
    protected Job getJob()
    {
        return this.deleteJob;
    }

    @Test
    void deleteFolder() throws Exception
    {
        Folder specs = mockFolder("Specs", "Resilience");
        File readme = mockFile("readme.txt", "Resilience");
        Folder resilience = mockFolder("Resilience", "Projects", Arrays.asList("Specs"), Arrays.asList("readme.txt"));
        Folder projects = mockFolder("Projects", null, Arrays.asList("Resilience"), Collections.<String> emptyList());

        DocumentReference specsReference = specs.getReference();
        doAnswer(updateChildFolders(resilience)).when(fileSystem).delete(specsReference);

        DocumentReference readmeReference = readme.getReference();
        doAnswer(updateChildFiles(resilience)).when(fileSystem).delete(readmeReference);

        DocumentReference resilienceReference = resilience.getReference();
        doAnswer(updateChildFolders(projects)).when(fileSystem).delete(resilienceReference);

        BatchPathRequest request = new BatchPathRequest();
        request.setPaths(Collections.singleton(new Path(projects.getReference())));

        execute(request);

        verify(fileSystem).delete(specs.getReference());
        verify(fileSystem).delete(readme.getReference());
        verify(fileSystem).delete(resilience.getReference());
        verify(fileSystem).delete(projects.getReference());
    }

    @Test
    void deleteProtectedFolder() throws Exception
    {
        File childFile = mockFile("readme.txt", "Projects");
        Folder childFolder = mockFolder("src", "Projects");
        Folder folder = mockFolder("Projects", null, Arrays.asList("src"), Arrays.asList("readme.txt"));

        when(fileSystem.canDelete(folder.getReference())).thenReturn(false);

        BatchPathRequest request = new BatchPathRequest();
        request.setPaths(Collections.singleton(new Path(folder.getReference())));

        execute(request);

        verify(fileSystem, never()).delete(childFile.getReference());
        verify(fileSystem, never()).delete(childFolder.getReference());
        verify(fileSystem, never()).delete(folder.getReference());
        assertEquals("You are not allowed to delete the folder [" + folder.getReference() + "].",
            this.logCapture.getMessage(0));
    }

    @Test
    void deleteFile() throws Exception
    {
        File file = mockFile("readme.txt", "Resilience", "Concerto");

        BatchPathRequest request = new BatchPathRequest();
        request.setPaths(Collections.singleton(new Path(ref("Resilience"), file.getReference())));

        execute(request);

        assertEquals(Arrays.asList("Concerto"), getParents(file));
        verify(fileSystem).save(file);
    }

    @Test
    void deleteFileFromAllParentFolders() throws Exception
    {
        File file = mockFile("readme.txt", "Resilience", "Concerto");

        BatchPathRequest request = new BatchPathRequest();
        request.setPaths(Collections.singleton(new Path(null, file.getReference())));

        execute(request);

        verify(fileSystem).delete(file.getReference());
    }

    @Test
    void deleteProtectedFiles() throws Exception
    {
        File readme = mockFile("readme.txt", "Resilience", "Concerto");
        when(fileSystem.canEdit(readme.getReference())).thenReturn(false);

        File pom = mockFile("pom.xml", "Concerto");
        when(fileSystem.canDelete(pom.getReference())).thenReturn(false);

        File index = mockFile("index.html", "Resilience");

        BatchPathRequest request = new BatchPathRequest();
        request.setPaths(Arrays.asList(new Path(ref("Resilience"), readme.getReference()), new Path(ref("Concerto"),
            pom.getReference()), new Path(ref("Resilience"), index.getReference())));

        execute(request);

        verify(fileSystem, never()).save(readme);
        assertEquals("You are not allowed to edit the file [" + readme.getReference() + "].",
            this.logCapture.getMessage(0));

        verify(fileSystem, never()).delete(pom.getReference());
        assertEquals("You are not allowed to delete the file [" + pom.getReference() + "].",
            this.logCapture.getMessage(1));

        verify(fileSystem).delete(index.getReference());
    }
}
