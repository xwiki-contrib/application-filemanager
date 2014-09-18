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

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.filemanager.File;
import org.xwiki.filemanager.Folder;
import org.xwiki.filemanager.Path;
import org.xwiki.filemanager.job.MoveRequest;
import org.xwiki.job.Job;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link MoveJob}.
 * 
 * @version $Id$
 * @since 2.0M1
 */
public class MoveJobTest extends AbstractJobTest
{
    @Rule
    public MockitoComponentMockingRule<Job> mocker = new MockitoComponentMockingRule<Job>(MoveJob.class);

    @Override
    protected MockitoComponentMockingRule<Job> getMocker()
    {
        return mocker;
    }

    @Test
    public void moveFolder() throws Exception
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
    public void moveFolderInItself() throws Exception
    {
        Folder child = mockFolder("Specs", "Resilience");
        mockFolder("Resilience", "Projects");
        Folder grandParent = mockFolder("Projects");

        MoveRequest request = new MoveRequest();
        request.setPaths(Collections.singleton(new Path(grandParent.getReference())));
        request.setDestination(new Path(child.getReference()));

        execute(request);

        verify(fileSystem, never()).save(grandParent);
        verify(mocker.getMockedLogger()).error("Cannot move [{}] to a sub-folder of itself.",
            grandParent.getReference());
    }

    @Test
    public void moveProtectedFolder() throws Exception
    {
        Folder source = mockFolder("Source");
        when(fileSystem.canEdit(source.getReference())).thenReturn(false);

        Folder destination = mockFolder("Destination");

        MoveRequest request = new MoveRequest();
        request.setPaths(Collections.singleton(new Path(source.getReference())));
        request.setDestination(new Path(destination.getReference()));

        execute(request);

        verify(fileSystem, never()).save(source);
        verify(mocker.getMockedLogger()).error("You are not allowed to move the folder [{}].", source.getReference());
    }

    @Test
    public void mergeFolder() throws Exception
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
    public void moveFile() throws Exception
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
    public void moveProtectedFile() throws Exception
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
        verify(mocker.getMockedLogger()).error("You are not allowed to move the file [{}].", file.getReference());
    }

    @Test
    public void overwriteFile() throws Exception
    {
        File pom = mockFile("pom.xml", "api");
        Folder api = mockFolder("api", null, Collections.<String>emptyList(), Arrays.asList("pom.xml"));

        File otherPom = mockFile("pom.xml1", "pom.xml", Arrays.asList("root"));
        Folder root = mockFolder("root", null, Collections.<String>emptyList(), Arrays.asList("pom.xml1"));

        MoveRequest request = new MoveRequest();
        request.setPaths(Collections.singleton(new Path(root.getReference(), otherPom.getReference())));
        request.setDestination(new Path(api.getReference()));

        request.setInteractive(true);
        Job job = mocker.getComponentUnderTest();
        answerOverwriteQuestion(job, true, false);

        job.initialize(request);
        job.run();

        verify(fileSystem).delete(pom.getReference());
    }

    @Test
    public void overwriteProtectedFile() throws Exception
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
        Job job = mocker.getComponentUnderTest();
        // Make sure the test doesn't hang waiting for the answer.
        answerOverwriteQuestion(job, true, false);

        job.initialize(request);
        job.run();

        verify(fileSystem, never()).delete(pom.getReference());
        verify(fileSystem, never()).save(otherPom);
    }

    @Test
    public void renameFolder() throws Exception
    {
        mockFolder("Projects");
        Folder folder = mockFolder("Concerto", "Projects", Arrays.asList("Specs"), Arrays.asList("readme.txt"));
        Folder childFolder = mockFolder("Specs", "Concerto");
        File childFile = mockFile("readme.txt", "Concerto", "Projects");

        // Test the unique ID counter.
        Folder otherFolder = mockFolder("Resilience");
        DocumentReference newReference = ref("Resilience1");
        generateReference(otherFolder.getReference(), newReference);

        MoveRequest request = new MoveRequest();
        request.setPaths(Collections.singleton(new Path(folder.getReference())));
        request.setDestination(new Path(null, otherFolder.getReference()));

        execute(request);

        verify(folder).setName(otherFolder.getName());
        verify(fileSystem).rename(folder, newReference);

        verify(childFolder).setParentReference(newReference);
        verify(fileSystem).save(childFolder);

        assertEquals(Arrays.asList("Projects", "Resilience1"), getParents(childFile));
        verify(fileSystem).save(childFile);
    }

    @Test
    public void renameProtectedFolder() throws Exception
    {
        mockFolder("Projects", null, Arrays.asList("Concerto"), Collections.<String>emptyList());
        Folder folder = mockFolder("Concerto", "Projects", Arrays.asList("Specs"), Arrays.asList("readme.txt"));
        Folder childFolder = mockFolder("Specs", "Concerto");
        File childFile = mockFile("readme.txt", "Concerto");

        when(fileSystem.canEdit(folder.getReference())).thenReturn(false);

        DocumentReference newReference = ref("Resilience");

        MoveRequest request = new MoveRequest();
        request.setPaths(Collections.singleton(new Path(folder.getReference())));
        request.setDestination(new Path(null, newReference));

        execute(request);

        verify(mocker.getMockedLogger()).error("You are not allowed to rename the folder [{}].", folder.getReference());
        verify(fileSystem, never()).rename(folder, newReference);
        verify(fileSystem, never()).save(childFolder);
        verify(fileSystem, never()).save(childFile);
    }

    @Test
    public void renameFolderUsingExistingName() throws Exception
    {
        Folder projects =
            mockFolder("Projects", null, Arrays.asList("Concerto", "Resilience"), Collections.<String>emptyList());
        Folder concerto = mockFolder("Concerto", "Projects");
        Folder resilience = mockFolder("Resilience", "Projects");

        MoveRequest request = new MoveRequest();
        request.setPaths(Collections.singleton(new Path(concerto.getReference())));
        request.setDestination(new Path(projects.getReference(), resilience.getReference()));

        execute(request);

        verify(mocker.getMockedLogger()).error("A folder with the same name [{}] already exists under [{}]",
            resilience.getName(), projects.getReference());
        verify(fileSystem, never()).rename(concerto, resilience.getReference());
    }

    @Test
    public void renameFile() throws Exception
    {
        mockFolder("Concerto");
        mockFolder("Resilience");
        File file = mockFile("readme.txt", "Concerto", "Resilience");

        DocumentReference newReference = ref("README");
        generateReference(newReference, newReference);

        MoveRequest request = new MoveRequest();
        request.setPaths(Collections.singleton(new Path(null, file.getReference())));
        request.setDestination(new Path(null, newReference));

        execute(request);

        verify(file).setName(newReference.getName());
        verify(fileSystem).rename(file, newReference);
    }

    @Test
    public void renameProtectedFile() throws Exception
    {
        File file = mockFile("readme.txt", "Concerto", "Resilience");
        when(fileSystem.canEdit(file.getReference())).thenReturn(false);

        DocumentReference newReference = ref("README");

        MoveRequest request = new MoveRequest();
        request.setPaths(Collections.singleton(new Path(null, file.getReference())));
        request.setDestination(new Path(null, newReference));

        execute(request);

        verify(fileSystem, never()).rename(file, newReference);
        verify(mocker.getMockedLogger()).error("You are not allowed to rename the file [{}].", file.getReference());
    }

    @Test
    public void renameFileUsingExistingName() throws Exception
    {
        File file = mockFile("readme.txt", "Concerto");
        File readme = mockFile("README", "Concerto");
        Folder folder =
            mockFolder("Concerto", null, Collections.<String>emptyList(), Arrays.asList("readme.txt", "README"));

        MoveRequest request = new MoveRequest();
        request.setPaths(Collections.singleton(new Path(null, file.getReference())));
        request.setDestination(new Path(null, readme.getReference()));

        execute(request);

        verify(fileSystem, never()).rename(file, readme.getReference());
        verify(mocker.getMockedLogger()).error("A file with the same name [{}] already exists under [{}]",
            readme.getName(), folder.getReference());
    }

    @Test
    public void moveAndRenameFile() throws Exception
    {
        File readme = mockFile("README", "Concerto");
        File file = mockFile("readme.txt", "Concerto", "Resilience");
        Folder concerto =
            mockFolder("Concerto", "Projects", Collections.<String>emptyList(), Arrays.asList("readme.txt", "README"));
        mockFolder("Resilience", "Projects", Collections.<String>emptyList(), Arrays.asList("readme.txt"));
        Folder projects =
            mockFolder("Projects", null, Arrays.asList("Concerto", "Resilience"), Collections.<String>emptyList());

        DocumentReference actualDestinationReference = ref("README1");
        generateReference(readme.getReference(), actualDestinationReference);

        MoveRequest request = new MoveRequest();
        request.setPaths(Collections.singleton(new Path(concerto.getReference(), file.getReference())));
        request.setDestination(new Path(projects.getReference(), readme.getReference()));

        execute(request);

        assertEquals(Arrays.asList("Resilience", "Projects"), getParents(file));

        verify(file).setName(readme.getName());
        verify(fileSystem).rename(file, actualDestinationReference);
        assertEquals(Arrays.asList("Resilience", "Projects"), getParents(file));
    }
}
