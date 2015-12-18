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
package org.xwiki.filemanager.job;

import java.util.Date;
import java.util.List;

import org.xwiki.job.Request;
import org.xwiki.job.event.status.JobProgress;
import org.xwiki.job.event.status.JobStatus;
import org.xwiki.logging.LogLevel;
import org.xwiki.logging.LogQueue;
import org.xwiki.logging.event.LogEvent;
import org.xwiki.stability.Unstable;

/**
 * Base class to implement a {@link JobStatus} that wraps another {@link JobStatus}.
 * 
 * @version $Id$
 * @since 2.0.5
 */
@Unstable
public class JobStatusAdapter implements JobStatus
{
    /**
     * The job status that is being wrapped and extended.
     */
    protected JobStatus jobStatus;

    /**
     * Creates a new job status by wrapping the given job status.
     * 
     * @param jobStatus the job status to wrap and extend
     */
    public JobStatusAdapter(JobStatus jobStatus)
    {
        this.jobStatus = jobStatus;
    }

    @Override
    public State getState()
    {
        return this.jobStatus.getState();
    }

    @Override
    public Request getRequest()
    {
        return this.jobStatus.getRequest();
    }

    @Override
    public LogQueue getLog()
    {
        return this.jobStatus.getLog();
    }

    @Override
    public JobProgress getProgress()
    {
        return this.jobStatus.getProgress();
    }

    @Override
    public void ask(Object question) throws InterruptedException
    {
        this.jobStatus.ask(question);
    }

    @Override
    public Object getQuestion()
    {
        return this.jobStatus.getQuestion();
    }

    @Override
    public void answered()
    {
        this.jobStatus.answered();
    }

    @Override
    public Date getStartDate()
    {
        return this.jobStatus.getStartDate();
    }

    @Override
    public Date getEndDate()
    {
        return this.jobStatus.getEndDate();
    }

    @Deprecated
    @Override
    public List<LogEvent> getLog(LogLevel level)
    {
        return this.jobStatus.getLog(level);
    }
}
