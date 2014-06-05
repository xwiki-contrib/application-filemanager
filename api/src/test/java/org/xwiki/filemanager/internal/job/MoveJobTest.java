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
import org.xwiki.job.Job;
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
        return mockFolder(new DocumentReference("wiki", "Drive", name), null);
    }

    private Folder mockFolder(String name, String parentName)
    {
        return mockFolder(name, parentName, Collections.<String> emptyList(), Collections.<String> emptyList());
    }

    private Folder mockFolder(String name, String parentName, List<String> childFolders, List<String> childFiles)
    {
        return mockFolder(new DocumentReference("wiki", "Drive", name), new DocumentReference("wiki", "Drive",
            parentName), asReference(childFolders), asReference(childFiles));
    }

    private Folder mockFolder(DocumentReference reference, DocumentReference parentReference)
    {
        return mockFolder(reference, parentReference, Collections.<DocumentReference> emptyList(),
            Collections.<DocumentReference> emptyList());
    }

    private Folder mockFolder(DocumentReference reference, DocumentReference parentReference,
        List<DocumentReference> childFolderReferences, List<DocumentReference> childFileReferences)
    {
        return mockFolder(reference, reference.getName(), parentReference, childFolderReferences, childFileReferences);
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
        DocumentReference reference = new DocumentReference("wiki", "Drive", name);
        return mockFile(reference, asReference(Arrays.asList(parents)));
    }

    private File mockFile(DocumentReference reference, Collection<DocumentReference> parentReferences)
    {
        return mockFile(reference, reference.getName(), parentReferences);
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

    private List<DocumentReference> asReference(List<String> names)
    {
        List<DocumentReference> references = new ArrayList<DocumentReference>();
        for (String name : names) {
            references.add(new DocumentReference("wiki", "Drive", name));
        }
        return references;
    }
}
