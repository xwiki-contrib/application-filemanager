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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.filemanager.reference.UniqueDocumentReferenceGenerator;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultUniqueDocumentReferenceGenerator}.
 * 
 * @version $Id$
 * @since 2.0RC1
 */
public class DefaultUniqueDocumentReferenceGeneratorTest
{
    @Rule
    public MockitoComponentMockingRule<UniqueDocumentReferenceGenerator> mocker =
        new MockitoComponentMockingRule<UniqueDocumentReferenceGenerator>(DefaultUniqueDocumentReferenceGenerator.class);

    private DocumentAccessBridge documentAccessBridge;

    @SuppressWarnings("unchecked")
    private Cache<Boolean> documentReferenceCache = mock(Cache.class);

    @BeforeComponent
    public void initializeCache() throws Exception
    {
        CacheManager cacheManager = this.mocker.registerMockComponent(CacheManager.class);
        when(cacheManager.<Boolean>createNewCache(any(CacheConfiguration.class))).thenReturn(
            this.documentReferenceCache);
    }

    @Before
    public void configure() throws Exception
    {
        this.documentAccessBridge = this.mocker.getInstance(DocumentAccessBridge.class);
    }

    @Test
    public void generate() throws Exception
    {
        DocumentReference baseReference = new DocumentReference("gang", "Drive", "foo");
        SpaceReference spaceReference = baseReference.getLastSpaceReference();

        when(this.documentAccessBridge.exists(baseReference)).thenReturn(true);
        when(this.documentReferenceCache.get("gang:Drive.foo")).thenReturn(null, true);
        when(this.documentReferenceCache.get("gang:Drive.foo1")).thenReturn(null, true);

        // Make sure we use a different sequence for each call, as if they are made by two different threads.
        DocumentReference firstReference =
            this.mocker.getComponentUnderTest().generate(spaceReference, new DocumentNameSequence("foo"));
        DocumentReference secondReference =
            this.mocker.getComponentUnderTest().generate(spaceReference, new DocumentNameSequence("foo"));

        verify(this.documentReferenceCache).set("gang:Drive.foo", true);
        verify(this.documentReferenceCache).set("gang:Drive.foo1", true);
        verify(this.documentReferenceCache).set("gang:Drive.foo2", true);

        assertEquals(spaceReference, firstReference.getLastSpaceReference());
        assertEquals("foo1", firstReference.getName());

        assertEquals(spaceReference, secondReference.getLastSpaceReference());
        assertEquals("foo2", secondReference.getName());
    }
}
