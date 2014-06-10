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

/**
 * Unit tests for {@link CopyJob}.
 * 
 * @version $Id$
 * @since 2.0M1
 */
public class CopyJobTest extends AbstractJobTest
{
    @Rule
    public MockitoComponentMockingRule<Job> mocker = new MockitoComponentMockingRule<Job>(CopyJob.class);

    @Override
    protected MockitoComponentMockingRule<Job> getMocker()
    {
        return mocker;
    }

    @Test
    public void copyFolder() throws Exception
    {
        File pom = mockFile("pom.xml", "Concerto");
        Folder specs = mockFolder("Specs", "Concerto");
        Folder concerto = mockFolder("Concerto", null, Arrays.asList("Specs"), Arrays.asList("pom.xml"));
        Folder projects = mockFolder("Projects");

        MoveRequest request = new MoveRequest();
        request.setPaths(Collections.singleton(new Path(concerto.getReference())));
        request.setDestination(new Path(projects.getReference()));

        mocker.getComponentUnderTest().start(request);

        DocumentReference concertoCopyRef = ref("Concerto1");
        verify(fileSystem).copy(concerto.getReference(), concertoCopyRef);

        Folder concertoCopy = fileSystem.getFolder(concertoCopyRef);
        verify(concertoCopy).setName("Concerto");
        verify(concertoCopy).setParentReference(projects.getReference());
        verify(fileSystem).save(concertoCopy);

        DocumentReference pomCopyRef = ref("pom.xml1");
        verify(fileSystem).copy(pom.getReference(), pomCopyRef);

        File pomCopy = fileSystem.getFile(pomCopyRef);
        verify(pomCopy).setName("pom.xml");
        verify(fileSystem).save(pomCopy);
        assertEquals(Arrays.asList("Concerto1"), getParents(pomCopy));

        DocumentReference specsCopyRef = ref("Specs1");
        verify(fileSystem).copy(specs.getReference(), specsCopyRef);

        Folder specsCopy = fileSystem.getFolder(specsCopyRef);
        verify(specsCopy).setName("Specs");
        verify(specsCopy).setParentReference(concertoCopy.getReference());
        verify(fileSystem).save(specsCopy);
    }

    @Test
    public void copyFolderAs() throws Exception
    {
        Folder projects = mockFolder("Projects");
        Folder concerto = mockFolder("Concerto", "Projects");
        DocumentReference concertoCopyRef = ref("Concerto Copy");

        MoveRequest request = new MoveRequest();
        request.setPaths(Collections.singleton(new Path(concerto.getReference())));
        request.setDestination(new Path(projects.getReference(), concertoCopyRef));

        mocker.getComponentUnderTest().start(request);

        verify(fileSystem).copy(concerto.getReference(), concertoCopyRef);

        Folder concertoCopy = fileSystem.getFolder(concertoCopyRef);
        verify(concertoCopy).setName("Concerto Copy");
        verify(concertoCopy).setParentReference(projects.getReference());
        verify(fileSystem).save(concertoCopy);
    }

    @Test
    public void copyFolderInItself() throws Exception
    {
        Folder projects = mockFolder("Projects");
        mockFolder("Resilience", "Projects");
        Folder specs = mockFolder("Specs", "Resilience");

        MoveRequest request = new MoveRequest();
        request.setPaths(Collections.singleton(new Path(projects.getReference())));
        request.setDestination(new Path(specs.getReference()));

        mocker.getComponentUnderTest().start(request);

        verify(fileSystem, never()).copy(eq(projects.getReference()), any(DocumentReference.class));
        verify(mocker.getMockedLogger()).error("Cannot copy [{}] to a sub-folder of itself.", projects.getReference());
    }

    @Test
    public void copyProtectedFolder() throws Exception
    {
        Folder projects = mockFolder("Projects");
        Folder resilience = mockFolder("Resilience");

        when(fileSystem.canView(resilience.getReference())).thenReturn(false);

        MoveRequest request = new MoveRequest();
        request.setPaths(Collections.singleton(new Path(resilience.getReference())));
        request.setDestination(new Path(projects.getReference()));

        mocker.getComponentUnderTest().start(request);

        verify(fileSystem, never()).copy(eq(resilience.getReference()), any(DocumentReference.class));
        verify(mocker.getMockedLogger()).error("You are not allowed to copy the folder [{}].",
            resilience.getReference());
    }

    @Test
    public void mergeFolder() throws Exception
    {
        File pom = mockFile("pom.xml", "Concerto");
        Folder specs = mockFolder("Specs", "Concerto");
        Folder concerto = mockFolder("Concerto", null, Arrays.asList("Specs"), Arrays.asList("pom.xml"));

        mockFolder("Specs1", "Specs", "Resilience", Collections.<String> emptyList(), Collections.<String> emptyList());
        Folder resilience = mockFolder("Resilience", null, Arrays.asList("Specs1"), Collections.<String> emptyList());

        Folder projects =
            mockFolder("Projects", null, Arrays.asList("Concerto", "Resilience"), Collections.<String> emptyList());

        MoveRequest request = new MoveRequest();
        request.setPaths(Collections.singleton(new Path(concerto.getReference())));
        request.setDestination(new Path(projects.getReference(), resilience.getReference()));

        mocker.getComponentUnderTest().start(request);

        verify(fileSystem, never()).copy(eq(concerto.getReference()), any(DocumentReference.class));
        verify(fileSystem, never()).copy(eq(specs.getReference()), any(DocumentReference.class));

        DocumentReference pomCopyRef = ref("pom.xml1");
        verify(fileSystem).copy(pom.getReference(), pomCopyRef);

        File pomCopy = fileSystem.getFile(pomCopyRef);
        verify(pomCopy).setName("pom.xml");
        verify(fileSystem).save(pomCopy);
        assertEquals(Arrays.asList("Resilience"), getParents(pomCopy));
    }

    @Test
    public void copyFile() throws Exception
    {
        File readme = mockFile("README", "Projects");
        Folder concerto = mockFolder("Concerto", "Projects");
        mockFolder("Projects", null, Arrays.asList("Concerto"), Arrays.asList("README"));

        MoveRequest request = new MoveRequest();
        request.setPaths(Collections.singleton(new Path(null, readme.getReference())));
        request.setDestination(new Path(concerto.getReference()));

        mocker.getComponentUnderTest().start(request);

        DocumentReference readmeCopyRef = ref("README1");
        verify(fileSystem).copy(readme.getReference(), readmeCopyRef);

        File readmeCopy = fileSystem.getFile(readmeCopyRef);
        verify(readmeCopy).setName("README");
        verify(fileSystem).save(readmeCopy);
        assertEquals(Arrays.asList("Concerto"), getParents(readmeCopy));
    }

    @Test
    public void copyFileAs() throws Exception
    {
        File readme = mockFile("README", "Projects");
        Folder projects = mockFolder("Projects", null, Collections.<String> emptyList(), Arrays.asList("README"));

        DocumentReference readmeCopyRef = ref("readme.txt");

        MoveRequest request = new MoveRequest();
        request.setPaths(Collections.singleton(new Path(null, readme.getReference())));
        request.setDestination(new Path(projects.getReference(), readmeCopyRef));

        mocker.getComponentUnderTest().start(request);

        verify(fileSystem).copy(readme.getReference(), readmeCopyRef);

        File readmeCopy = fileSystem.getFile(readmeCopyRef);
        verify(readmeCopy).setName("readme.txt");
        verify(fileSystem).save(readmeCopy);
        assertEquals(Arrays.asList("Projects"), getParents(readmeCopy));
    }

    @Test
    public void copyProtectedFile() throws Exception
    {
        File readme = mockFile("README");
        Folder concerto = mockFolder("Concerto");

        when(fileSystem.canView(readme.getReference())).thenReturn(false);

        MoveRequest request = new MoveRequest();
        request.setPaths(Collections.singleton(new Path(null, readme.getReference())));
        request.setDestination(new Path(concerto.getReference()));

        mocker.getComponentUnderTest().start(request);

        verify(fileSystem, never()).copy(eq(readme.getReference()), any(DocumentReference.class));
        verify(mocker.getMockedLogger()).error("You are not allowed to copy the file [{}].", readme.getReference());
    }

    @Test
    public void overwriteFile() throws Exception
    {
        File pom = mockFile("pom.xml", "Resilience");
        Folder resilience = mockFolder("Resilience", null, Collections.<String> emptyList(), Arrays.asList("pom.xml"));
        File otherPom = mockFile("pom.xml1", "pom.xml", Collections.<String> emptyList());

        MoveRequest request = new MoveRequest();
        request.setPaths(Collections.singleton(new Path(null, otherPom.getReference())));
        request.setDestination(new Path(resilience.getReference()));

        request.setInteractive(true);
        Job job = mocker.getComponentUnderTest();
        answerOverwriteQuestion(job, true, false);

        job.start(request);

        verify(fileSystem).delete(pom.getReference());

        DocumentReference otherPomCopyRef = ref("pom.xml2");
        verify(fileSystem).copy(otherPom.getReference(), otherPomCopyRef);

        File otherPomCopy = fileSystem.getFile(otherPomCopyRef);
        verify(otherPomCopy).setName("pom.xml");
        verify(fileSystem).save(otherPomCopy);
        assertEquals(Arrays.asList("Resilience"), getParents(otherPomCopy));
    }

    @Test
    public void overwriteProtectedFile() throws Exception
    {
        File pom = mockFile("pom.xml", "Resilience");
        Folder resilience = mockFolder("Resilience", null, Collections.<String> emptyList(), Arrays.asList("pom.xml"));
        File otherPom = mockFile("pom.xml1", "pom.xml", Collections.<String> emptyList());

        when(fileSystem.canDelete(pom.getReference())).thenReturn(false);

        MoveRequest request = new MoveRequest();
        request.setPaths(Collections.singleton(new Path(null, otherPom.getReference())));
        request.setDestination(new Path(resilience.getReference()));

        request.setInteractive(true);
        Job job = mocker.getComponentUnderTest();
        answerOverwriteQuestion(job, true, false);

        job.start(request);

        verify(fileSystem, never()).delete(pom.getReference());
        verify(fileSystem, never()).copy(eq(otherPom.getReference()), any(DocumentReference.class));
    }
}
