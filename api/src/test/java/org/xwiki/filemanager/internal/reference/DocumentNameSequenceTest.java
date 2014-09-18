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

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link DocumentNameSequence}.
 * 
 * @version $Id$
 * @since 2.0RC1
 */
public class DocumentNameSequenceTest
{
    @Test
    public void iterate()
    {
        DocumentNameSequence sequence = new DocumentNameSequence("foo");
        int size = 106;
        List<String> names = new ArrayList<String>(size);
        for (int i = 0; i < size; i++) {
            assertTrue(sequence.hasNext());
            names.add(sequence.next());
        }

        // Counter
        assertEquals("foo", names.get(0));
        assertEquals("foo1", names.get(1));
        assertEquals("foo99", names.get(99));
        // Random number
        assertTrue(names.get(100).matches("foo\\d+{3,6}"));
        assertTrue(names.get(104).matches("foo\\d+{3,6}"));
        // UUID
        assertTrue(names.get(105).matches("foo[a-f0-9]+{8}-[a-f0-9]+{4}-[a-f0-9]+{4}-[a-f0-9]+{4}-[a-f0-9]+{12}"));
    }

    @Test
    public void remove()
    {
        DocumentNameSequence sequence = new DocumentNameSequence("bar");
        try {
            sequence.remove();
            fail();
        } catch (UnsupportedOperationException e) {
            // Expected.
        }
    }
}
