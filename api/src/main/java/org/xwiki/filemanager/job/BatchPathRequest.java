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

import java.util.Collection;

import org.xwiki.filemanager.Path;
import org.xwiki.job.AbstractRequest;
import org.xwiki.stability.Unstable;

/**
 * A generic request that targets multiple paths in the file system.
 * 
 * @version $Id$
 * @since 2.0M1
 */
@Unstable
public class BatchPathRequest extends AbstractRequest
{
    /**
     * @see #getPaths()
     */
    public static final String PROPERTY_PATHS = "paths";

    /**
     * Serialization identifier.
     */
    private static final long serialVersionUID = 1L;

    /**
     * @return the collection of paths that are targeted by this request
     */
    public Collection<Path> getPaths()
    {
        return getProperty(PROPERTY_PATHS);
    }

    /**
     * Sets the paths that are targeted by this request.
     * 
     * @param paths the collection of paths that are targeted by this request
     */
    public void setPaths(Collection<Path> paths)
    {
        setProperty(PROPERTY_PATHS, paths);
    }
}
