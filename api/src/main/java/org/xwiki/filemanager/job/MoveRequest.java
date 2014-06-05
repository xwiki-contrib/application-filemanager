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

import org.xwiki.filemanager.Path;

/**
 * Request used by {@link org.xwiki.filemanager.internal.job.MoveJob} to move files and folders to a different path,
 * possibly renaming the target file or folder in case there is only one item to move.
 * 
 * @version $Id$
 * @since 2.0M1
 */
public class MoveRequest extends BatchPathRequest
{
    /**
     * @see #getDestination()
     */
    public static final String PROPERTY_DESTINATION = "destination";

    /**
     * Serialization identifier.
     */
    private static final long serialVersionUID = 1L;

    /**
     * @return the destination path where to move the files and folders
     */
    public Path getDestination()
    {
        return getProperty(PROPERTY_DESTINATION);
    }

    /**
     * Sets the destination path where to move the files and folders.
     * 
     * @param destination the destination path
     */
    public void setDestination(Path destination)
    {
        setProperty(PROPERTY_DESTINATION, destination);
    }
}
