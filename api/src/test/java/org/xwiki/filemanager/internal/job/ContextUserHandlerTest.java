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
import java.util.List;

import javax.inject.Provider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.filemanager.job.FileManager;
import org.xwiki.job.Request;
import org.xwiki.job.event.JobFinishedEvent;
import org.xwiki.job.event.JobStartedEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.EventListener;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.XWikiContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ContextUserHandler}.
 * 
 * @version $Id$
 * @since 2.0M1
 */
public class ContextUserHandlerTest
{
    @Rule
    public MockitoComponentMockingRule<EventListener> mocker = new MockitoComponentMockingRule<EventListener>(
        ContextUserHandler.class);

    private XWikiContext xcontext;

    @Before
    public void configure() throws Exception
    {
        xcontext = mock(XWikiContext.class);
        Provider<XWikiContext> xcontextProvider = mocker.getInstance(XWikiContext.TYPE_PROVIDER);
        when(xcontextProvider.get()).thenReturn(xcontext);
    }

    @Test
    public void setUserReferenceOnJobStarted() throws Exception
    {
        DocumentReference userReference = new DocumentReference("wiki", "Users", "mflorea");
        List<String> jobId = Arrays.asList(FileManager.JOB_ID_PREFIX, "xyz");

        Request request = mock(Request.class);
        when(request.getProperty("user.reference")).thenReturn(userReference);

        mocker.getComponentUnderTest().onEvent(new JobStartedEvent(jobId, null, request), null, null);

        verify(xcontext).setUserReference(userReference);
    }

    @Test
    public void resetUserReferenceOnJobFinished() throws Exception
    {
        List<String> jobId = Arrays.asList(FileManager.JOB_ID_PREFIX, "abc");
        mocker.getComponentUnderTest().onEvent(new JobFinishedEvent(jobId, null, null), null, null);

        verify(xcontext).setUserReference(null);
    }

    @Test
    public void ignoreNonFileSystemJobs() throws Exception
    {
        List<String> jobId = Arrays.asList("foo", "abc");
        mocker.getComponentUnderTest().onEvent(new JobStartedEvent(jobId, null, null), null, null);
        mocker.getComponentUnderTest().onEvent(new JobFinishedEvent(jobId, null, null), null, null);

        verify(xcontext, never()).setUserReference(any(DocumentReference.class));
    }
}
