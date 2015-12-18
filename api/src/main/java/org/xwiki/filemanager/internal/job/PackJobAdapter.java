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

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.job.Job;
import org.xwiki.job.event.status.JobStatus;

/**
 * Wraps the actual {@link PackJob} in order to add custom data to the {@link org.xwiki.job.internal.DefaultJobStatus}
 * without extending the class. We still need to use {@link org.xwiki.job.internal.DefaultJobStatus} because
 * {@link org.xwiki.job.internal.AbstractJob} uses it and it implements a lot of helper methods that simplify the
 * creation of a job.
 * 
 * @version $Id$
 * @since 2.0.5
 */
@Component
@Named(PackJob.JOB_TYPE)
public class PackJobAdapter extends AbstractJobAdapter
{
    @Inject
    @Named(PackJob.JOB_TYPE + "/actual")
    private Job actualPackJob;

    @Override
    protected Job getJob()
    {
        return actualPackJob;
    }

    @Override
    public JobStatus getStatus()
    {
        if (this.actualPackJob instanceof PackJob) {
            return ((PackJob) this.actualPackJob).getPackStatus();
        }
        return super.getStatus();
    }
}
