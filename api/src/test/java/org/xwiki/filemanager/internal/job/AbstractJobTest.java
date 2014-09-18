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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.filemanager.Document;
import org.xwiki.filemanager.File;
import org.xwiki.filemanager.FileSystem;
import org.xwiki.filemanager.Folder;
import org.xwiki.filemanager.internal.reference.DocumentNameSequence;
import org.xwiki.filemanager.job.OverwriteQuestion;
import org.xwiki.filemanager.reference.UniqueDocumentReferenceGenerator;
import org.xwiki.job.Job;
import org.xwiki.job.Request;
import org.xwiki.job.event.status.JobStatus;
import org.xwiki.job.event.status.JobStatus.State;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

/**
 * Base class for file manager job tests.
 * 
 * @version $Id$
 * @since 2.0M1
 */
public abstract class AbstractJobTest
{
    protected static class UpdateChildFiles implements Answer<Void>
    {
        private Folder folder;

        private List<DocumentReference> childFiles;

        public UpdateChildFiles(Folder folder, List<DocumentReference> childFiles)
        {
            this.folder = folder;
            this.childFiles = childFiles;
        }

        @Override
        public Void answer(InvocationOnMock invocation) throws Throwable
        {
            when(folder.getChildFileReferences()).thenReturn(childFiles);
            return null;
        }
    }

    protected static class UpdateChildFolders implements Answer<Void>
    {
        private Folder folder;

        private List<DocumentReference> childFolders;

        public UpdateChildFolders(Folder folder, List<DocumentReference> childFolders)
        {
            this.folder = folder;
            this.childFolders = childFolders;
        }

        @Override
        public Void answer(InvocationOnMock invocation) throws Throwable
        {
            when(folder.getChildFolderReferences()).thenReturn(childFolders);
            return null;
        }
    }

    protected FileSystem fileSystem;

    @Before
    public void configure() throws Exception
    {
        fileSystem = getMocker().getInstance(FileSystem.class);

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

        doAnswer(new Answer<Void>()
        {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable
            {
                DocumentReference source = (DocumentReference) invocation.getArguments()[0];
                DocumentReference destination = (DocumentReference) invocation.getArguments()[1];
                File file = fileSystem.getFile(source);
                Folder folder = fileSystem.getFolder(source);
                if (file != null) {
                    mockFile(destination, file.getName(), new ArrayList<DocumentReference>(file.getParentReferences()));
                } else if (folder != null) {
                    mockFolder(destination, folder.getName(), folder.getParentReference(),
                        Collections.<DocumentReference>emptyList(), Collections.<DocumentReference>emptyList());
                }
                return null;
            }

        }).when(fileSystem).copy(any(DocumentReference.class), any(DocumentReference.class));
    }

    protected abstract MockitoComponentMockingRule<Job> getMocker();

    protected Folder mockFolder(String name)
    {
        return mockFolder(name, null);
    }

    protected Folder mockFolder(String name, String parentName)
    {
        return mockFolder(name, parentName, Collections.<String>emptyList(), Collections.<String>emptyList());
    }

    protected Folder mockFolder(String name, String parentId, List<String> childFolders, List<String> childFiles)
    {
        return mockFolder(name, name, parentId, childFolders, childFiles);
    }

    protected Folder mockFolder(String id, String name, String parentId, List<String> childFolders,
        List<String> childFiles)
    {
        DocumentReference parentReference = parentId != null ? ref(parentId) : null;
        return mockFolder(ref(id), name, parentReference, ref(childFolders), ref(childFiles));
    }

    protected Folder mockFolder(DocumentReference reference, String name, DocumentReference parentReference,
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
        when(fileSystem.canView(reference)).thenReturn(true);
        when(fileSystem.canEdit(reference)).thenReturn(true);
        when(fileSystem.canDelete(reference)).thenReturn(true);

        return folder;
    }

    protected File mockFile(String name, String... parents)
    {
        return mockFile(name, name, Arrays.asList(parents));
    }

    protected File mockFile(String id, String name, Collection<String> parentIds)
    {
        return mockFile(ref(id), name, ref(parentIds));
    }

    protected File mockFile(DocumentReference reference, String name, Collection<DocumentReference> parentReferences)
    {
        File file = mock(File.class, reference.toString());
        when(file.getReference()).thenReturn(reference);
        when(file.getName()).thenReturn(name);
        when(file.getParentReferences()).thenReturn(parentReferences);

        when(fileSystem.exists(reference)).thenReturn(true);
        when(fileSystem.getFile(reference)).thenReturn(file);
        when(fileSystem.canView(reference)).thenReturn(true);
        when(fileSystem.canEdit(reference)).thenReturn(true);
        when(fileSystem.canDelete(reference)).thenReturn(true);

        return file;
    }

    protected Collection<String> getParents(File file)
    {
        Collection<String> parents = new ArrayList<String>();
        for (DocumentReference parentReference : file.getParentReferences()) {
            parents.add(parentReference.getName());
        }
        return parents;
    }

    protected List<DocumentReference> ref(Collection<String> names)
    {
        List<DocumentReference> references = new ArrayList<DocumentReference>();
        for (String name : names) {
            references.add(ref(name));
        }
        return references;
    }

    protected DocumentReference ref(String id)
    {
        return new DocumentReference("wiki", "Drive", id);
    }

    protected void answerOverwriteQuestion(final Job job, final boolean overwrite, final boolean askAgain)
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

    protected UpdateChildFiles updateChildFiles(Folder folder, String... fileIds)
    {
        return new UpdateChildFiles(folder, ref(Arrays.asList(fileIds)));
    }

    protected UpdateChildFolders updateChildFolders(Folder folder, String... folderIds)
    {
        return new UpdateChildFolders(folder, ref(Arrays.asList(folderIds)));
    }

    protected Job execute(Request request) throws Exception
    {
        Job job = getMocker().getComponentUnderTest();
        job.initialize(request);
        job.run();
        return job;
    }

    protected void generateReference(DocumentReference base, DocumentReference result) throws Exception
    {
        UniqueDocumentReferenceGenerator generator = getMocker().getInstance(UniqueDocumentReferenceGenerator.class);
        when(generator.generate(base.getLastSpaceReference(), new DocumentNameSequence(base.getName()))).thenReturn(
            result);
    }
}
