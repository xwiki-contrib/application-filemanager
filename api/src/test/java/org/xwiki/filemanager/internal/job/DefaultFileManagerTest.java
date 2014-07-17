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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Queue;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.filemanager.Path;
import org.xwiki.filemanager.job.BatchPathRequest;
import org.xwiki.filemanager.job.FileManager;
import org.xwiki.filemanager.job.MoveRequest;
import org.xwiki.job.JobManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.EventListener;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

/**
 * Unit tests for {@link DefaultFileManager}.
 * 
 * @version $Id$
 * @since 2.0M1
 */
public class DefaultFileManagerTest
{
    @Rule
    public MockitoComponentMockingRule<FileManager> mocker = new MockitoComponentMockingRule<FileManager>(
        DefaultFileManager.class);

    private JobManager jobManager;

    private Queue<String> activeJobQueue;

    private DocumentReference currentUserReference = new DocumentReference("wiki", "Users", "mflorea");

    @SuppressWarnings("unchecked")
    @Before
    public void configure() throws Exception
    {
        jobManager = mocker.getInstance(JobManager.class);

        activeJobQueue = (Queue<String>) mock(EventListener.class, withSettings().extraInterfaces(Queue.class));
        mocker.registerComponent(EventListener.class, "ActiveFileSystemJobQueue", activeJobQueue);

        DocumentAccessBridge documentAccessBridge = mocker.getInstance(DocumentAccessBridge.class);
        when(documentAccessBridge.getCurrentUserReference()).thenReturn(currentUserReference);
    }

    @Test
    public void move() throws Exception
    {
        Path destination = new Path(null);
        Collection<Path> paths = Collections.singleton(new Path(null));

        String jobId = mocker.getComponentUnderTest().move(paths, destination);

        ArgumentCaptor<MoveRequest> request = ArgumentCaptor.forClass(MoveRequest.class);
        verify(jobManager).addJob(eq(MoveJob.JOB_TYPE), request.capture());
        assertEquals(Arrays.asList(FileManager.JOB_ID_PREFIX, jobId), request.getValue().getId());
        assertArrayEquals(paths.toArray(), request.getValue().getPaths().toArray());
        assertEquals(destination, request.getValue().getDestination());
        assertEquals(currentUserReference, request.getValue().getProperty("user.reference"));
        assertEquals(MoveJob.JOB_TYPE, request.getValue().getProperty("job.type"));
        assertFalse(request.getValue().isInteractive());

        verify(activeJobQueue).offer(jobId);
    }

    @Test
    public void copy() throws Exception
    {
        Path destination = new Path(null);
        Collection<Path> paths = Collections.singleton(new Path(null));

        String jobId = mocker.getComponentUnderTest().copy(paths, destination);

        ArgumentCaptor<MoveRequest> request = ArgumentCaptor.forClass(MoveRequest.class);
        verify(jobManager).addJob(eq(CopyJob.JOB_TYPE), request.capture());
        assertEquals(Arrays.asList(FileManager.JOB_ID_PREFIX, jobId), request.getValue().getId());
        assertArrayEquals(paths.toArray(), request.getValue().getPaths().toArray());
        assertEquals(destination, request.getValue().getDestination());
        assertEquals(currentUserReference, request.getValue().getProperty("user.reference"));
        assertEquals(CopyJob.JOB_TYPE, request.getValue().getProperty("job.type"));
        assertFalse(request.getValue().isInteractive());

        verify(activeJobQueue).offer(jobId);
    }

    @Test
    public void delete() throws Exception
    {
        Collection<Path> paths = Collections.singleton(new Path(null));
        String jobId = mocker.getComponentUnderTest().delete(paths);

        ArgumentCaptor<BatchPathRequest> request = ArgumentCaptor.forClass(BatchPathRequest.class);
        verify(jobManager).addJob(eq(DeleteJob.JOB_TYPE), request.capture());
        assertEquals(Arrays.asList(FileManager.JOB_ID_PREFIX, jobId), request.getValue().getId());
        assertArrayEquals(paths.toArray(), request.getValue().getPaths().toArray());
        assertEquals(currentUserReference, request.getValue().getProperty("user.reference"));
        assertEquals(DeleteJob.JOB_TYPE, request.getValue().getProperty("job.type"));
        assertFalse(request.getValue().isInteractive());

        verify(activeJobQueue).offer(jobId);
    }
}
