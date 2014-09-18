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
package org.xwiki.filemanager.reference;

import java.util.Iterator;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.stability.Unstable;

/**
 * Interface used to generate a unique {@link DocumentReference}, i.e. a reference to a document that doesn't exist.
 * 
 * @version $Id$
 * @since 2.0RC1
 */
@Role
@Unstable
public interface UniqueDocumentReferenceGenerator
{
    /**
     * Iterates the given sequence of document names until it finds one that doesn't exist already in the specified
     * space or that isn't already reserved.
     * 
     * @param spaceReference the space where the generated document name must be unique
     * @param documentNameSequence a sequence of document names that will be iterated until a document name that is
     *            unique in the specified space is found
     * @return the reference to the first document in the given sequence that doesn't exist already in the specified
     *         space, or that isn't already reserved
     */
    DocumentReference generate(SpaceReference spaceReference, Iterator<String> documentNameSequence);
}
