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

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.filemanager.Document;
import org.xwiki.filemanager.File;
import org.xwiki.filemanager.FileSystem;
import org.xwiki.filemanager.Folder;
import org.xwiki.filemanager.Path;
import org.xwiki.filemanager.job.MoveRequest;
import org.xwiki.filemanager.job.OverwriteQuestion;
import org.xwiki.job.Job;
import org.xwiki.job.event.status.JobStatus;
import org.xwiki.job.event.status.JobStatus.State;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

/**
 * Unit tests for {@link MoveJob}.
 * 
 * @version $Id$
 * @since 2.0M1
 */
public class MoveJobTest
{
    @Rule
    public MockitoComponentMockingRule<Job> mocker = new MockitoComponentMockingRule<Job>(MoveJob.class);

    private FileSystem fileSystem;

    @Before
    public void configure() throws Exception
    {
        fileSystem = mocker.getInstance(FileSystem.class);

        doAnswer(new Answer<Void>()
        {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable
            {
                Document document = (Document) invocation.getArguments()[0];
                DocumentReference reference = (DocumentReference) invocation.getArguments()[1];
                when(document.getReference()).thenReturn(reference);
                return null;
            }

        }).when(fileSystem).rename(any(Document.class), any(DocumentReference.class));
    }

    @Test
    public void moveFolder() throws Exception
    {
        Folder folder = mockFolder("Concerto", "Projects");
        Folder newParent = mockFolder("Retired Projects");

        MoveRequest request = new MoveRequest();
        request.setPaths(Collections.singleton(new Path(folder.getReference())));
        request.setDestination(new Path(newParent.getReference()));

        mocker.getComponentUnderTest().start(request);

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

        mocker.getComponentUnderTest().start(request);

        verify(mocker.getMockedLogger()).error("Cannot move [{}] to a sub-folder of itself.",
            grandParent.getReference());
    }

    @Test
    public void mergeFolder() throws Exception
    {
        mockFolder("Tests", "Concerto");
        mockFolder("Specs", "Concerto");
        Folder concerto =
            mockFolder("Concerto", "Projects", Arrays.asList("Specs", "Tests"), Collections.<String> emptyList());
        Folder projects = mockFolder("Projects", null, Arrays.asList("Concerto"), Collections.<String> emptyList());

        final File testFile = mockFile("test.in", "TestsNew");
        final Folder testsNew =
            mockFolder("TestsNew", "Tests", "ConcertoNew", Collections.<String> emptyList(), Arrays.asList("test.in"));
        final Folder src = mockFolder("src", "ConcertoNew");
        final Folder concertoNew =
            mockFolder("ConcertoNew", "Concerto", null, Arrays.asList("src", "TestsNew"),
                Collections.<String> emptyList());

        // Assume that testFile is saved after the parent is updated (we verify this at the end).
        doAnswer(new Answer<Void>()
        {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable
            {
                when(testsNew.getChildFileReferences()).thenReturn(Collections.<DocumentReference> emptyList());
                return null;
            }
        }).when(fileSystem).save(testFile);

        // ConcertoNew should remain empty after its only child is deleted.
        DocumentReference testsNewReference = testsNew.getReference();
        doAnswer(new Answer<Void>()
        {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable
            {
                when(concertoNew.getChildFolderReferences()).thenReturn(Collections.<DocumentReference> emptyList());
                return null;
            }
        }).when(fileSystem).delete(testsNewReference);

        MoveRequest request = new MoveRequest();
        request.setPaths(Collections.singleton(new Path(concertoNew.getReference())));
        request.setDestination(new Path(projects.getReference()));

        mocker.getComponentUnderTest().start(request);

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

        mocker.getComponentUnderTest().start(request);

        assertEquals(Arrays.asList("Resilience", "Projects"), getParents(file));

        verify(fileSystem).save(file);
    }

    @Test
    public void overwriteFile() throws Exception
    {
        File pom = mockFile("pom.xml", "api");
        Folder api = mockFolder("api", null, Collections.<String> emptyList(), Arrays.asList("pom.xml"));

        File otherPom = mockFile("pom.xml1", "pom.xml", Arrays.asList("root"));
        Folder root = mockFolder("root", null, Collections.<String> emptyList(), Arrays.asList("pom.xml1"));

        MoveRequest request = new MoveRequest();
        request.setPaths(Collections.singleton(new Path(root.getReference(), otherPom.getReference())));
        request.setDestination(new Path(api.getReference()));

        request.setInteractive(true);
        Job job = mocker.getComponentUnderTest();
        answerOverwriteQuestion(job, true, false);

        job.start(request);

        verify(fileSystem).delete(pom.getReference());
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
        DocumentReference newReference = new DocumentReference("wiki", "Drive", "Resilience1");

        MoveRequest request = new MoveRequest();
        request.setPaths(Collections.singleton(new Path(folder.getReference())));
        request.setDestination(new Path(null, otherFolder.getReference()));

        mocker.getComponentUnderTest().start(request);

        verify(folder).setName(otherFolder.getName());
        verify(fileSystem).rename(folder, newReference);

        verify(childFolder).setParentReference(newReference);
        verify(fileSystem).save(childFolder);

        assertEquals(Arrays.asList("Projects", "Resilience1"), getParents(childFile));
        verify(fileSystem).save(childFile);
    }

    @Test
    public void renameFile() throws Exception
    {
        mockFolder("Concerto");
        mockFolder("Resilience");
        File file = mockFile("readme.txt", "Concerto", "Resilience");

        DocumentReference newReference = new DocumentReference("wiki", "Drive", "README");

        MoveRequest request = new MoveRequest();
        request.setPaths(Collections.singleton(new Path(null, file.getReference())));
        request.setDestination(new Path(null, newReference));

        mocker.getComponentUnderTest().start(request);

        verify(file).setName(newReference.getName());
        verify(fileSystem).rename(file, newReference);
    }

    private Folder mockFolder(String name)
    {
        return mockFolder(name, null);
    }

    private Folder mockFolder(String name, String parentName)
    {
        return mockFolder(name, parentName, Collections.<String> emptyList(), Collections.<String> emptyList());
    }

    private Folder mockFolder(String name, String parentId, List<String> childFolders, List<String> childFiles)
    {
        return mockFolder(name, name, parentId, childFolders, childFiles);
    }

    private Folder mockFolder(String id, String name, String parentId, List<String> childFolders,
        List<String> childFiles)
    {
        DocumentReference parentReference = parentId != null ? ref(parentId) : null;
        return mockFolder(ref(id), name, parentReference, ref(childFolders), ref(childFiles));
    }

    private Folder mockFolder(DocumentReference reference, String name, DocumentReference parentReference,
        List<DocumentReference> childFolderReferences, List<DocumentReference> childFileReferences)
    {
        Folder folder = mock(Folder.class, reference.toString());
        when(folder.getReference()).thenReturn(reference);
        when(folder.getName()).thenReturn(name);
        when(folder.getParentReference()).thenReturn(parentReference);
        when(folder.getChildFolderReferences()).thenReturn(childFolderReferences);
        when(folder.getChildFileReferences()).thenReturn(childFileReferences);

        when(fileSystem.exists(reference)).thenReturn(true);
        when(fileSystem.getFolder(reference)).thenReturn(folder);
        when(fileSystem.canEdit(reference)).thenReturn(true);

        return folder;
    }

    private File mockFile(String name, String... parents)
    {
        return mockFile(name, name, Arrays.asList(parents));
    }

    private File mockFile(String id, String name, Collection<String> parentIds)
    {
        return mockFile(ref(id), name, ref(parentIds));
    }

    private File mockFile(DocumentReference reference, String name, Collection<DocumentReference> parentReferences)
    {
        File file = mock(File.class, reference.toString());
        when(file.getReference()).thenReturn(reference);
        when(file.getName()).thenReturn(name);
        when(file.getParentReferences()).thenReturn(parentReferences);

        when(fileSystem.exists(reference)).thenReturn(true);
        when(fileSystem.getFile(reference)).thenReturn(file);
        when(fileSystem.canEdit(reference)).thenReturn(true);

        return file;
    }

    private Collection<String> getParents(File file)
    {
        Collection<String> parents = new ArrayList<String>();
        for (DocumentReference parentReference : file.getParentReferences()) {
            parents.add(parentReference.getName());
        }
        return parents;
    }

    private List<DocumentReference> ref(Collection<String> names)
    {
        List<DocumentReference> references = new ArrayList<DocumentReference>();
        for (String name : names) {
            references.add(ref(name));
        }
        return references;
    }

    private DocumentReference ref(String id)
    {
        return new DocumentReference("wiki", "Drive", id);
    }

    private void answerOverwriteQuestion(final Job job, final boolean overwrite, final boolean askAgain)
        throws Exception
    {
        new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                for (int i = 0; i < 5; i++) {
                    try {
                        Thread.sleep(20);
                        JobStatus status = job.getStatus();
                        if (status != null && status.getState() == State.WAITING) {
                            OverwriteQuestion question = (OverwriteQuestion) status.getQuestion();
                            question.setOverwrite(overwrite);
                            question.setAskAgain(askAgain);
                            status.answered();
                            return;
                        }
                    } catch (Exception e) {
                    }
                }
            }
        }, "Answer Overwrite Question").start();
    }
}
