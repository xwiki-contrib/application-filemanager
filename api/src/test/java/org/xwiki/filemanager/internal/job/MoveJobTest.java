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
import org.xwiki.filemanager.job.MoveRequest;
import org.xwiki.job.Job;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link MoveJob}.
 * 
 * @version $Id$
 * @since 2.0M1
 */
@ComponentTest
class MoveJobTest extends AbstractJobTest
{
    @InjectMockComponents
    private MoveJob moveJob;

    @Override
    protected Job getJob()
    {
        return this.moveJob;
    }

    @Test
    void moveFolder() throws Exception
    {
        Folder folder = mockFolder("Concerto", "Projects");
        Folder newParent = mockFolder("Retired Projects");

        MoveRequest request = new MoveRequest();
        request.setPaths(Collections.singleton(new Path(folder.getReference())));
        request.setDestination(new Path(newParent.getReference()));

        execute(request);

        verify(folder).setParentReference(newParent.getReference());
        verify(fileSystem).save(folder);
    }

    @Test
    void moveFolderInItself() throws Exception
    {
        Folder child = mockFolder("Specs", "Resilience");
        mockFolder("Resilience", "Projects");
        Folder grandParent = mockFolder("Projects");

        MoveRequest request = new MoveRequest();
        request.setPaths(Collections.singleton(new Path(grandParent.getReference())));
        request.setDestination(new Path(child.getReference()));

        execute(request);

        verify(fileSystem, never()).save(grandParent);
        assertEquals("Cannot move [" + grandParent.getReference() + "] to a sub-folder of itself.",
            this.logCapture.getMessage(0));
    }

    @Test
    void moveProtectedFolder() throws Exception
    {
        Folder source = mockFolder("Source");
        when(fileSystem.canEdit(source.getReference())).thenReturn(false);

        Folder destination = mockFolder("Destination");

        MoveRequest request = new MoveRequest();
        request.setPaths(Collections.singleton(new Path(source.getReference())));
        request.setDestination(new Path(destination.getReference()));

        execute(request);

        verify(fileSystem, never()).save(source);
        assertEquals("You are not allowed to move the folder [" + source.getReference() + "].",
            this.logCapture.getMessage(0));
    }

    @Test
    void mergeFolder() throws Exception
    {
        mockFolder("Tests", "Concerto");
        mockFolder("Specs", "Concerto");
        Folder concerto =
            mockFolder("Concerto", "Projects", Arrays.asList("Specs", "Tests"), Collections.<String>emptyList());
        Folder projects = mockFolder("Projects", null, Arrays.asList("Concerto"), Collections.<String>emptyList());

        final File testFile = mockFile("test.in", "TestsNew");
        final Folder testsNew =
            mockFolder("TestsNew", "Tests", "ConcertoNew", Collections.<String>emptyList(), Arrays.asList("test.in"));
        final Folder src = mockFolder("src", "ConcertoNew");
        final Folder concertoNew =
            mockFolder("ConcertoNew", "Concerto", null, Arrays.asList("src", "TestsNew"),
                Collections.<String>emptyList());

        // Assume that testFile is saved after the parent is updated (we verify this at the end).
        doAnswer(updateChildFiles(testsNew)).when(fileSystem).save(testFile);

        // ConcertoNew should remain empty after its only child is deleted.
        DocumentReference testsNewReference = testsNew.getReference();
        doAnswer(updateChildFolders(concertoNew)).when(fileSystem).delete(testsNewReference);

        MoveRequest request = new MoveRequest();
        request.setPaths(Collections.singleton(new Path(concertoNew.getReference())));
        request.setDestination(new Path(projects.getReference()));

        execute(request);

        assertEquals(Collections.singletonList("Tests"), getParents(testFile));

        verify(src).setParentReference(concerto.getReference());
        verify(fileSystem).save(src);

        verify(fileSystem).delete(concertoNew.getReference());
    }

    @Test
    void moveFile() throws Exception
    {
        File file = mockFile("readme.txt", "Concerto", "Resilience");
        Folder newParent = mockFolder("Projects");

        MoveRequest request = new MoveRequest();
        DocumentReference oldParentReference = file.getParentReferences().iterator().next();
        request.setPaths(Collections.singleton(new Path(oldParentReference, file.getReference())));
        request.setDestination(new Path(newParent.getReference()));

        execute(request);

        assertEquals(Arrays.asList("Resilience", "Projects"), getParents(file));

        verify(fileSystem).save(file);
    }

    @Test
    void moveProtectedFile() throws Exception
    {
        File file = mockFile("readme.txt", "Concerto", "Resilience");
        when(fileSystem.canEdit(file.getReference())).thenReturn(false);

        Folder newParent = mockFolder("Projects");

        MoveRequest request = new MoveRequest();
        DocumentReference oldParentReference = file.getParentReferences().iterator().next();
        request.setPaths(Collections.singleton(new Path(oldParentReference, file.getReference())));
        request.setDestination(new Path(newParent.getReference()));

        execute(request);

        verify(fileSystem, never()).save(file);
        assertEquals("You are not allowed to move the file [" + file.getReference() + "].",
            this.logCapture.getMessage(0));
    }

    @Test
    void overwriteFile() throws Exception
    {
        File pom = mockFile("pom.xml", "api");
        Folder api = mockFolder("api", null, Collections.<String>emptyList(), Arrays.asList("pom.xml"));

        File otherPom = mockFile("pom.xml1", "pom.xml", Arrays.asList("root"));
        Folder root = mockFolder("root", null, Collections.<String>emptyList(), Arrays.asList("pom.xml1"));

        MoveRequest request = new MoveRequest();
        request.setPaths(Collections.singleton(new Path(root.getReference(), otherPom.getReference())));
        request.setDestination(new Path(api.getReference()));

        request.setInteractive(true);
        Job job = getJob();
        answerOverwriteQuestion(job, true, false);

        job.initialize(request);
        job.run();

        verify(fileSystem).delete(pom.getReference());
    }

    @Test
    void overwriteProtectedFile() throws Exception
    {
        File pom = mockFile("pom.xml", "api");
        when(fileSystem.canDelete(pom.getReference())).thenReturn(false);
        Folder api = mockFolder("api", null, Collections.<String>emptyList(), Arrays.asList("pom.xml"));

        File otherPom = mockFile("pom.xml1", "pom.xml", Arrays.asList("root"));
        Folder root = mockFolder("root", null, Collections.<String>emptyList(), Arrays.asList("pom.xml1"));

        MoveRequest request = new MoveRequest();
        request.setPaths(Collections.singleton(new Path(root.getReference(), otherPom.getReference())));
        request.setDestination(new Path(api.getReference()));

        request.setInteractive(true);
        Job job = getJob();
        // Make sure the test doesn't hang waiting for the answer.
        answerOverwriteQuestion(job, true, false);

        job.initialize(request);
        job.run();

        verify(fileSystem, never()).delete(pom.getReference());
        verify(fileSystem, never()).save(otherPom);
        assertEquals("You are not allowed to overwrite the file [" + pom.getReference() + "].",
            this.logCapture.getMessage(0));
    }

    @Test
    void renameFolder() throws Exception
    {
        mockFolder("Projects");
        Folder folder = mockFolder("Concerto", "Projects", Arrays.asList("Specs"), Arrays.asList("readme.txt"));
        Folder childFolder = mockFolder("Specs", "Concerto");
        File childFile = mockFile("readme.txt", "Concerto", "Projects");

        // Test the unique ID counter.
        Folder otherFolder = mockFolder("Resilience");
        Folder newFolder =
            mockFolder("Resilience1", "Concerto", "Projects", Arrays.asList("Specs"), Arrays.asList("readme.txt"));
        DocumentReference newReference = newFolder.getReference();
        generateReference(otherFolder.getReference(), newReference);

        MoveRequest request = new MoveRequest();
        request.setPaths(Collections.singleton(new Path(folder.getReference())));
        request.setDestination(new Path(null, otherFolder.getReference()));

        execute(request);

        verify(fileSystem).rename(folder.getReference(), newReference);
        verify(newFolder).setName(otherFolder.getName());
        verify(fileSystem).save(newFolder);

        verify(childFolder).setParentReference(newReference);
        verify(fileSystem).save(childFolder);

        assertEquals(Arrays.asList("Projects", "Resilience1"), getParents(childFile));
        verify(fileSystem).save(childFile);
    }

    @Test
    void renameProtectedFolder() throws Exception
    {
        mockFolder("Projects", null, Arrays.asList("Concerto"), Collections.<String>emptyList());
        Folder folder = mockFolder("Concerto", "Projects", Arrays.asList("Specs"), Arrays.asList("readme.txt"));
        Folder childFolder = mockFolder("Specs", "Concerto");
        File childFile = mockFile("readme.txt", "Concerto");

        when(fileSystem.canDelete(folder.getReference())).thenReturn(false);

        DocumentReference newReference = ref("Resilience");

        MoveRequest request = new MoveRequest();
        request.setPaths(Collections.singleton(new Path(folder.getReference())));
        request.setDestination(new Path(null, newReference));

        execute(request);

        assertEquals("You are not allowed to rename the folder [" + folder.getReference() + "].",
            this.logCapture.getMessage(0));
        verify(fileSystem, never()).rename(folder.getReference(), newReference);
        verify(fileSystem, never()).save(childFolder);
        verify(fileSystem, never()).save(childFile);
    }

    @Test
    void renameFolderUsingProtectedReference() throws Exception
    {
        mockFolder("Projects", null, Arrays.asList("Concerto"), Collections.<String>emptyList());
        Folder folder = mockFolder("Concerto", "Projects", Arrays.asList("Specs"), Arrays.asList("readme.txt"));
        Folder childFolder = mockFolder("Specs", "Concerto");
        File childFile = mockFile("readme.txt", "Concerto");

        DocumentReference newReference = ref("Resilience");
        generateReference(newReference, newReference);
        when(fileSystem.canEdit(newReference)).thenReturn(false);

        MoveRequest request = new MoveRequest();
        request.setPaths(Collections.singleton(new Path(folder.getReference())));
        request.setDestination(new Path(null, newReference));

        execute(request);

        assertEquals("You are not allowed to create the folder [" + newReference + "].",
            this.logCapture.getMessage(0));
        verify(fileSystem, never()).rename(eq(folder.getReference()), any(DocumentReference.class));
        verify(fileSystem, never()).save(childFolder);
        verify(fileSystem, never()).save(childFile);
    }

    @Test
    void renameFolderUsingExistingName() throws Exception
    {
        Folder projects =
            mockFolder("Projects", null, Arrays.asList("Concerto", "Resilience"), Collections.<String>emptyList());
        Folder concerto = mockFolder("Concerto", "Projects");
        Folder resilience = mockFolder("Resilience", "Projects");

        MoveRequest request = new MoveRequest();
        request.setPaths(Collections.singleton(new Path(concerto.getReference())));
        request.setDestination(new Path(projects.getReference(), resilience.getReference()));

        execute(request);

        assertEquals("A folder with the same name [" + resilience.getName() + "] already exists under ["
            + projects.getReference() + "]", this.logCapture.getMessage(0));
        verify(fileSystem, never()).rename(concerto.getReference(), resilience.getReference());
    }

    @Test
    void renameFile() throws Exception
    {
        mockFolder("Concerto");
        mockFolder("Resilience");
        File file = mockFile("readme.txt", "Concerto", "Resilience");

        File newFile = mockFile("README", "readme.txt", Arrays.asList("Concerto", "Resilience"));
        DocumentReference newReference = newFile.getReference();

        generateReference(newReference, newReference);

        MoveRequest request = new MoveRequest();
        request.setPaths(Collections.singleton(new Path(null, file.getReference())));
        request.setDestination(new Path(null, newReference));

        execute(request);

        verify(fileSystem).rename(file.getReference(), newReference);
        verify(newFile).setName(newReference.getName());
        verify(fileSystem).save(newFile);
    }

    @Test
    void renameProtectedFile() throws Exception
    {
        File file = mockFile("readme.txt", "Concerto", "Resilience");
        when(fileSystem.canDelete(file.getReference())).thenReturn(false);

        DocumentReference newReference = ref("README");

        MoveRequest request = new MoveRequest();
        request.setPaths(Collections.singleton(new Path(null, file.getReference())));
        request.setDestination(new Path(null, newReference));

        execute(request);

        verify(fileSystem, never()).rename(file.getReference(), newReference);
        assertEquals("You are not allowed to rename the file [" + file.getReference() + "].",
            this.logCapture.getMessage(0));
    }

    @Test
    void renameFileUsingProtectedReference() throws Exception
    {
        File file = mockFile("readme.txt", "Concerto", "Resilience");

        DocumentReference newReference = ref("README");
        generateReference(newReference, newReference);
        when(fileSystem.canEdit(newReference)).thenReturn(false);

        MoveRequest request = new MoveRequest();
        request.setPaths(Collections.singleton(new Path(null, file.getReference())));
        request.setDestination(new Path(null, newReference));

        execute(request);

        verify(fileSystem, never()).rename(eq(file.getReference()), any(DocumentReference.class));
        assertEquals("You are not allowed to create the file [" + newReference + "].",
            this.logCapture.getMessage(0));
    }

    @Test
    void renameFileUsingExistingName() throws Exception
    {
        File file = mockFile("readme.txt", "Concerto");
        File readme = mockFile("README", "Concerto");
        Folder folder =
            mockFolder("Concerto", null, Collections.<String>emptyList(), Arrays.asList("readme.txt", "README"));

        MoveRequest request = new MoveRequest();
        request.setPaths(Collections.singleton(new Path(null, file.getReference())));
        request.setDestination(new Path(null, readme.getReference()));

        execute(request);

        verify(fileSystem, never()).rename(file.getReference(), readme.getReference());
        assertEquals("A file with the same name [" + readme.getName() + "] already exists under ["
            + folder.getReference() + "]", this.logCapture.getMessage(0));
    }

    @Test
    void moveAndRenameFile() throws Exception
    {
        File readme = mockFile("README", "Concerto");
        File file = mockFile("readme.txt", "Concerto", "Resilience");
        Folder concerto =
            mockFolder("Concerto", "Projects", Collections.<String>emptyList(), Arrays.asList("readme.txt", "README"));
        mockFolder("Resilience", "Projects", Collections.<String>emptyList(), Arrays.asList("readme.txt"));
        Folder projects =
            mockFolder("Projects", null, Arrays.asList("Concerto", "Resilience"), Collections.<String>emptyList());

        File newFile = mockFile("README1", "readme.txt", Arrays.asList("Concerto", "Resilience"));
        DocumentReference actualDestinationReference = newFile.getReference();
        generateReference(readme.getReference(), actualDestinationReference);

        MoveRequest request = new MoveRequest();
        request.setPaths(Collections.singleton(new Path(concerto.getReference(), file.getReference())));
        request.setDestination(new Path(projects.getReference(), readme.getReference()));

        execute(request);

        assertEquals(Arrays.asList("Resilience", "Projects"), getParents(file));
        verify(fileSystem).rename(file.getReference(), actualDestinationReference);
        verify(newFile).setName(readme.getName());
        verify(fileSystem).save(newFile);
    }
}
