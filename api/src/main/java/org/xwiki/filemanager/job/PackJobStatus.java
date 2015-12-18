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

import java.io.Serializable;

import org.xwiki.job.event.status.JobStatus;
import org.xwiki.stability.Unstable;

/**
 * The status of a job that executes a {@link PackRequest}.
 * 
 * @version $Id$
 * @since 2.0M2
 */
@Unstable
public class PackJobStatus extends JobStatusAdapter implements Serializable
{
    /**
     * Serialization identifier.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The number of bytes written so far in the output ZIP file.
     */
    private long bytesWritten;

    /**
     * The actual size (in bytes) of the output file, after the ZIP compression.
     */
    private long outputFileSize;

    /**
     * Creates a new job status by extending the provided (default) job status.
     * 
     * @param jobStatus the (default) job status to extend
     */
    public PackJobStatus(JobStatus jobStatus)
    {
        super(jobStatus);
    }

    /**
     * @return the number of bytes written so far in the output ZIP file
     */
    public long getBytesWritten()
    {
        return bytesWritten;
    }

    /**
     * Sets the number of bytes written so far in the output ZIP file.
     * 
     * @param bytesWritten the number of bytes written so far
     */
    public void setBytesWritten(long bytesWritten)
    {
        this.bytesWritten = bytesWritten;
    }

    /**
     * @return the actual size (in bytes) of the output file, after the ZIP compression
     */
    public long getOutputFileSize()
    {
        return outputFileSize;
    }

    /**
     * Sets the actual size (in bytes) of the output file, after the ZIP compression.
     * 
     * @param outputFileSize the size, in bytes, of the output ZIP file
     */
    public void setOutputFileSize(long outputFileSize)
    {
        this.outputFileSize = outputFileSize;
    }
}
