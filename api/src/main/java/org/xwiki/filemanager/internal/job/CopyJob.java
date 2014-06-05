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

import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.filemanager.job.MoveRequest;
import org.xwiki.job.AbstractJob;

/**
 * Copy files and folders to a different parent, possibly with a different name if the target list contains one item.
 * 
 * @version $Id$
 * @since 2.0M1
 */
@Component
@Named(CopyJob.JOB_TYPE)
public class CopyJob extends AbstractJob<MoveRequest>
{
    /**
     * The id of the job.
     */
    public static final String JOB_TYPE = "copy";

    @Override
    public String getType()
    {
        return JOB_TYPE;
    }

    @Override
    protected void start() throws Exception
    {
    }
}
