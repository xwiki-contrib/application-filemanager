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

import java.util.Arrays;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.filemanager.job.FileManager;
import org.xwiki.job.event.JobFinishedEvent;
import org.xwiki.observation.EventListener;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

/**
 * Unit tests for {@link ActiveJobQueue}.
 * 
 * @version $Id$
 * @since 2.0M1
 */
public class ActiveJobQueueTest
{
    @Rule
    public MockitoComponentMockingRule<EventListener> mocker = new MockitoComponentMockingRule<EventListener>(
        ActiveJobQueue.class);

    @Test
    public void removeFinishedJobs() throws Exception
    {
        ActiveJobQueue activeJobQueue = (ActiveJobQueue) mocker.getComponentUnderTest();
        activeJobQueue.add("abc");

        List<String> jobId = Arrays.asList(FileManager.JOB_ID_PREFIX, "abc");
        activeJobQueue.onEvent(new JobFinishedEvent(jobId, null, null), null, null);

        assertTrue(activeJobQueue.isEmpty());
    }

    @Test
    public void ignoreUnkownJobs() throws Exception
    {
        ActiveJobQueue activeJobQueue = (ActiveJobQueue) mocker.getComponentUnderTest();
        activeJobQueue.add("xyz");

        List<String> firstJobId = Arrays.asList(FileManager.JOB_ID_PREFIX, "abc");
        activeJobQueue.onEvent(new JobFinishedEvent(firstJobId, null, null), null, null);

        List<String> secondJobId = Arrays.asList("foo", "bar");
        activeJobQueue.onEvent(new JobFinishedEvent(secondJobId, null, null), null, null);

        assertEquals(1, activeJobQueue.size());
        assertEquals("xyz", activeJobQueue.peek());
    }
}
