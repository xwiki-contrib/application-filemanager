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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.job.event.JobEvent;
import org.xwiki.job.event.JobFinishedEvent;
import org.xwiki.job.event.JobStartedEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.XWikiContext;

/**
 * Sets the context user before job execution.
 * 
 * @version $Id$
 * @since 2.0M1
 */
@Component
@Named(ContextUserHandler.NAME)
@Singleton
public class ContextUserHandler implements EventListener
{
    /**
     * The name of the event listener.
     */
    public static final String NAME = "FileSystemJobContextUserHandler";

    /**
     * Provides the XWiki context to set the current user.
     */
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Override
    public List<Event> getEvents()
    {
        return Arrays.<Event> asList(new JobStartedEvent(), new JobFinishedEvent());
    }

    @Override
    public String getName()
    {
        return NAME;
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (event instanceof JobEvent) {
            List<String> jobId = ((JobEvent) event).getJobId();
            if (jobId != null && jobId.size() == 2 && DefaultFileManager.JOB_ID_PREFIX.equals(jobId.get(0))) {
                if (event instanceof JobStartedEvent) {
                    xcontextProvider.get().setUserReference(
                        ((JobStartedEvent) event).getRequest().<DocumentReference> getProperty("user.reference"));
                } else {
                    xcontextProvider.get().setUserReference(null);
                }
            }
        }
    }
}
