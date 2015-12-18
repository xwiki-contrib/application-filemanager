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

import java.util.concurrent.TimeUnit;

import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.job.Job;
import org.xwiki.job.Request;
import org.xwiki.job.event.status.JobStatus;

/**
 * Base class for implementing a {@link Job} that wraps another {@link Job}.
 * 
 * @version $Id$
 * @since 2.0.5
 */
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public abstract class AbstractJobAdapter implements Job
{
    /**
     * @return the wrapped job
     */
    protected abstract Job getJob();

    @Override
    public void run()
    {
        getJob().run();
    }

    @Override
    public String getType()
    {
        return getJob().getType();
    }

    @Override
    public JobStatus getStatus()
    {
        return getJob().getStatus();
    }

    @Override
    public Request getRequest()
    {
        return getJob().getRequest();
    }

    @Deprecated
    @Override
    public void start(Request request)
    {
        getJob().start(request);
    }

    @Override
    public void initialize(Request request)
    {
        getJob().initialize(request);
    }

    @Override
    public void join() throws InterruptedException
    {
        getJob().join();
    }

    @Override
    public boolean join(long time, TimeUnit unit) throws InterruptedException
    {
        return getJob().join(time, unit);
    }
}
