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
package org.xwiki.filemanager.script;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.filemanager.Path;
import org.xwiki.filemanager.internal.reference.DocumentNameSequence;
import org.xwiki.filemanager.job.BatchPathRequest;
import org.xwiki.filemanager.job.FileManager;
import org.xwiki.filemanager.reference.UniqueDocumentReferenceGenerator;
import org.xwiki.job.event.status.JobStatus;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.script.service.ScriptService;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DriveScriptService}.
 * 
 * @version $Id$
 * @since 2.0RC1
 */
public class DriveScriptServiceTest
{
    @Rule
    public MockitoComponentMockingRule<ScriptService> mocker = new MockitoComponentMockingRule<ScriptService>(
        DriveScriptService.class);

    private DriveScriptService drive;

    private SpaceReference driveReference = new SpaceReference("Drive", new WikiReference("wiki"));

    private FileManager fileManager;

    @Before
    public void configure() throws Exception
    {
        this.drive = (DriveScriptService) this.mocker.getComponentUnderTest();

        DocumentAccessBridge dab = this.mocker.getInstance(DocumentAccessBridge.class);
        when(dab.getCurrentDocumentReference()).thenReturn(new DocumentReference("page", driveReference));

        Execution execution = this.mocker.getInstance(Execution.class);
        when(execution.getContext()).thenReturn(mock(ExecutionContext.class));

        this.fileManager = this.mocker.getInstance(FileManager.class);
    }

    @Test
    public void move() throws Exception
    {
        Collection<Path> paths =
            Arrays.asList(newPath("Concerto", "pom.xml"), newPath(null, "readme.txt"), newPath("Resilience"));
        Path destination = newPath("Projects");
        when(this.fileManager.move(paths, destination)).thenReturn("abc");

        assertEquals("abc", drive.move(Arrays.asList("Concerto/pom.xml", "/readme.txt", "Resilience"), "Projects"));
    }

    @Test
    public void getActiveJobs()
    {
        when(fileManager.getActiveJobs()).thenReturn(Arrays.asList("a", "b", "c", "d", "e"));

        JobStatus bJobStatus = mock(JobStatus.class, "b");
        when(fileManager.getJobStatus("b")).thenReturn(bJobStatus);
        when(bJobStatus.getRequest()).thenReturn(new BatchPathRequest());

        JobStatus cJobStatus = mock(JobStatus.class, "c");
        when(fileManager.getJobStatus("c")).thenReturn(cJobStatus);
        BatchPathRequest cRequest = new BatchPathRequest();
        cRequest.setPaths(Arrays.asList(newPath("foo")));
        when(cJobStatus.getRequest()).thenReturn(cRequest);

        JobStatus dJobStatus = mock(JobStatus.class, "d");
        when(fileManager.getJobStatus("d")).thenReturn(dJobStatus);
        BatchPathRequest dRequest = new BatchPathRequest();
        dRequest.setPaths(Arrays.asList(new Path(new DocumentReference("math", "Drive", "video.mp4"))));
        when(dJobStatus.getRequest()).thenReturn(dRequest);

        JobStatus eJobStatus = mock(JobStatus.class, "e");
        when(fileManager.getJobStatus("e")).thenReturn(eJobStatus);
        BatchPathRequest eRequest = new BatchPathRequest();
        eRequest.setPaths(Arrays.asList(newPath(null, "bar")));
        when(eJobStatus.getRequest()).thenReturn(eRequest);

        assertEquals(Arrays.asList("c", "e"), this.drive.getActiveJobs());
    }

    @Test
    public void getUniqueReference() throws Exception
    {
        UniqueDocumentReferenceGenerator uniqueDocRefGenerator =
            this.mocker.getInstance(UniqueDocumentReferenceGenerator.class);
        DocumentReference expectedReference = newReference("bar");
        when(uniqueDocRefGenerator.generate(this.driveReference, new DocumentNameSequence("foo"))).thenReturn(
            expectedReference);

        assertEquals(expectedReference, this.drive.getUniqueReference("foo"));
    }

    private DocumentReference newReference(String name)
    {
        return new DocumentReference(name, driveReference);
    }

    private Path newPath(String parent, String child)
    {
        return new Path(parent != null ? newReference(parent) : null, newReference(child));
    }

    private Path newPath(String name)
    {
        return new Path(newReference(name));
    }
}
