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

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.cache.eviction.LRUEvictionConfiguration;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.phase.Disposable;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.filemanager.reference.UniqueDocumentReferenceGenerator;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;

/**
 * Implements {@link UniqueDocumentReferenceGenerator} using a cache to reserve document references for a period of
 * time.
 * 
 * @version $Id$
 * @since 2.0RC1
 */
@Component
@Singleton
public class DefaultUniqueDocumentReferenceGenerator
    implements UniqueDocumentReferenceGenerator, Initializable, Disposable
{
    /**
     * Used to check if a document exists.
     */
    @Inject
    private DocumentAccessBridge documentAccessBridge;

    /**
     * Used to create the cache.
     */
    @Inject
    private CacheManager cacheManager;

    /**
     * Used to cache (reserve) document references.
     */
    private Cache<Boolean> documentReferenceCache;

    @Override
    public synchronized DocumentReference generate(SpaceReference spaceReference, Iterator<String> documentNameSequence)
    {
        while (documentNameSequence.hasNext()) {
            String name = documentNameSequence.next();
            DocumentReference reference = new DocumentReference(name, spaceReference);
            String key = reference.toString();
            if (this.documentReferenceCache.get(key) == null) {
                // The reference is not reserved.
                this.documentReferenceCache.set(key, true);
                if (!this.documentAccessBridge.exists(reference)) {
                    return reference;
                }
            }
        }
        return null;
    }

    @Override
    public void initialize() throws InitializationException
    {
        // Initialize the cache.
        CacheConfiguration cacheConfiguration = new CacheConfiguration();
        cacheConfiguration.setConfigurationId("unique.documentReference");
        LRUEvictionConfiguration lru = new LRUEvictionConfiguration();
        lru.setMaxEntries(1000);
        // Discard after 1 hour.
        lru.setTimeToLive(3600);
        cacheConfiguration.put(LRUEvictionConfiguration.CONFIGURATIONID, lru);

        try {
            this.documentReferenceCache = this.cacheManager.createNewCache(cacheConfiguration);
        } catch (CacheException e) {
            throw new InitializationException("Failed to initialize the document reference cache.", e);
        }
    }

    @Override
    public void dispose() throws ComponentLifecycleException
    {
        if (this.documentReferenceCache != null) {
            this.documentReferenceCache.dispose();
        }
    }
}
