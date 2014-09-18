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
package org.xwiki.filemanager.internal.reference;

import java.util.Iterator;
import java.util.UUID;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Generates a sequence of document names starting from a given document name and adding a suffix which is either a
 * counter, a random number or an {@link UUID}.
 * 
 * @version $Id$
 * @since 2.0RC1
 */
public class DocumentNameSequence implements Iterator<String>
{
    /**
     * The base document name. Each item in the sequence (except the first) will be constructed from this base name by
     * adding a suffix, which is either a counter, a random name of an {@link UUID}.
     */
    private final String base;

    /**
     * The amount of consecutive numbers (starting with 1) that have been used so far in the sequence.
     */
    private int consecutiveCounter;

    /**
     * The amount of random numbers that have been used so far in the sequence.
     */
    private int randomCounter;

    /**
     * Creates a new sequence based on the specified document name. Each item in the sequence (except the first) will be
     * constructed from this base name by adding a suffix, which is either a counter, a random name of an {@link UUID}.
     * 
     * @param base the base document name
     */
    public DocumentNameSequence(String base)
    {
        this.base = base;
    }

    @Override
    public boolean hasNext()
    {
        // Infinite sequence.
        return true;
    }

    @Override
    public String next()
    {
        String name = this.base;
        if (this.consecutiveCounter == 0) {
            this.consecutiveCounter++;
        } else if (this.consecutiveCounter < 100) {
            name += this.consecutiveCounter++;
        } else if (this.randomCounter < 5) {
            name += 100 + (int) (Math.random() * 100000);
            this.randomCounter++;
        } else {
            name += UUID.randomUUID();
        }

        return name;
    }

    @Override
    public void remove()
    {
        // Read-only sequence.
        throw new UnsupportedOperationException();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(13, 7).append(this.base).toHashCode();
    }

    @Override
    public boolean equals(Object object)
    {
        if (object == null) {
            return false;
        } else if (object == this) {
            return true;
        } else if (object.getClass() != getClass()) {
            return false;
        }
        DocumentNameSequence other = (DocumentNameSequence) object;
        return new EqualsBuilder().append(this.base, other.base).isEquals();
    }
}
