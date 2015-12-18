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

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.io.IOUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.xwiki.environment.Environment;
import org.xwiki.filemanager.File;
import org.xwiki.filemanager.Folder;
import org.xwiki.filemanager.Path;
import org.xwiki.filemanager.job.PackRequest;
import org.xwiki.job.Job;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link PackJob}.
 * 
 * @version $Id$
 * @since 2.0M2
 */
public class PackJobTest extends AbstractJobTest
{
    @Rule
    public MockitoComponentMockingRule<Job> mocker = new MockitoComponentMockingRule<Job>(PackJob.class);

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    @Override
    protected MockitoComponentMockingRule<Job> getMocker()
    {
        return mocker;
    }

    @Override
    public void configure() throws Exception
    {
        super.configure();

        Environment environment = mocker.getInstance(Environment.class);
        when(environment.getTemporaryDirectory()).thenReturn(testFolder.getRoot());
    }

    @Test
    public void pack() throws Exception
    {
        Folder projects = mockFolder("Projects", "Pr\u00F4j\u00EA\u00E7\u021B\u0219", null,
            Arrays.asList("Concerto", "Resilience"), Arrays.asList("key.pub"));
        File key = mockFile("key.pub", "Projects");
        when(fileSystem.canView(key.getReference())).thenReturn(false);

        mockFolder("Concerto", "Projects", Collections.<String> emptyList(), Arrays.asList("pom.xml"));
        File pom = mockFile("pom.xml", "m&y p?o#m.x=m$l", "Concerto");
        setFileContent(pom, "foo");

        Folder resilience = mockFolder("Resilience", "Projects", Arrays.asList("src"), Arrays.asList("build.xml"));
        when(fileSystem.canView(resilience.getReference())).thenReturn(false);
        mockFolder("src", "Resilience");
        mockFile("build.xml");

        File readme = mockFile("readme.txt", "r\u00E9\u00E0dm\u00E8.txt");
        setFileContent(readme, "blah");

        PackRequest request = new PackRequest();
        request.setPaths(Arrays.asList(new Path(projects.getReference()), new Path(null, readme.getReference())));
        request.setOutputFileReference(new AttachmentReference("out.zip",
            new DocumentReference("wiki", "Space", "Page")));

        PackJob job = (PackJob) execute(request);

        ZipFile zip = new ZipFile(new java.io.File(testFolder.getRoot(), "temp/filemanager/wiki/Space/Page/out.zip"));
        List<String> folders = new ArrayList<String>();
        Map<String, String> files = new HashMap<String, String>();
        Enumeration<ZipArchiveEntry> entries = zip.getEntries();
        while (entries.hasMoreElements()) {
            ZipArchiveEntry entry = entries.nextElement();
            if (entry.isDirectory()) {
                folders.add(entry.getName());
            } else if (zip.canReadEntryData(entry)) {
                StringWriter writer = new StringWriter();
                IOUtils.copy(zip.getInputStream(entry), writer);
                files.put(entry.getName(), writer.toString());
            }
        }
        zip.close();

        assertEquals(Arrays.asList(projects.getName() + '/', projects.getName() + "/Concerto/"), folders);
        assertEquals(2, files.size());
        assertEquals("blah", files.get(readme.getName()));
        assertEquals("foo", files.get(projects.getName() + "/Concerto/" + pom.getName()));
        assertEquals(("blah" + "foo").getBytes().length, job.getPackStatus().getBytesWritten());
        assertTrue(job.getPackStatus().getOutputFileSize() > 0);
    }

    private void setFileContent(File file, String content)
    {
        when(file.getContent()).thenReturn(new ByteArrayInputStream(content.getBytes()));
    }
}
