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

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.stability.Unstable;

/**
 * Question asked when a file with the same name is found during a copy or move operation and we don't know whether to
 * overwrite or keep the existing file.
 * 
 * @version $Id$
 * @since 2.0M1
 */
@Unstable
public class OverwriteQuestion
{
    /**
     * The file being copied or moved.
     */
    private final DocumentReference source;

    /**
     * A file with the same name that exists in the destination folder.
     */
    private final DocumentReference destination;

    /**
     * Whether to overwrite or not the file that exists in the destination folder with the one being copied or moved.
     */
    private boolean overwrite = true;

    /**
     * Whether this question will be asked again or not if another file with the same name is found.
     */
    private boolean askAgain = true;

    /**
     * Ask whether to overwrite or not the destination file with the source file.
     * 
     * @param source the file being copied or moved
     * @param destination a file with the same name that exists in the destination folder
     */
    public OverwriteQuestion(DocumentReference source, DocumentReference destination)
    {
        this.source = source;
        this.destination = destination;
    }

    /**
     * @return the file that is being copied or moved
     */
    public DocumentReference getSource()
    {
        return source;
    }

    /**
     * @return a file with the same name that exists in the destination folder
     */
    public DocumentReference getDestination()
    {
        return destination;
    }

    /**
     * @return {@code true} to overwrite the file that exists in the destination folder with the one being copied or
     *         moved, {@code false} to keep the exiting file
     */
    public boolean isOverwrite()
    {
        return overwrite;
    }

    /**
     * Sets whether to overwrite or not the file that exists in the destination folder with the one being copied or
     * moved.
     * 
     * @param overwrite {@code true} to overwrite, {@code false} to keep
     */
    public void setOverwrite(boolean overwrite)
    {
        this.overwrite = overwrite;
    }

    /**
     * @return whether this question will be asked again or not if another file with the same name is found.
     */
    public boolean isAskAgain()
    {
        return askAgain;
    }

    /**
     * Sets whether this question will be asked again or not if another file with the same name is found.
     * 
     * @param askAgain {@code true} to ask again, {@code false} to perform the same action for the following files,
     *            during the current operation
     */
    public void setAskAgain(boolean askAgain)
    {
        this.askAgain = askAgain;
    }
}
