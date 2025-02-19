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
package com.xwiki.documentation.internal;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;

import com.xwiki.documentation.DocumentationBridge;
import com.xwiki.documentation.DocumentationException;
import com.xwiki.documentation.SectionParentManager;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;

/**
 * This is the default implementation of {@link SectionParentManager}.
 *
 * @version $Id$
 * @since 1.0
 */
@Component
@Singleton
public class DefaultSectionParentManager implements SectionParentManager
{
    @Inject
    private DocumentationBridge documentationBridge;

    @Override
    public void computeParentSection(DocumentReference documentReference) throws DocumentationException
    {
        // Verify that a parent section exists with the correct name.
        // Compute the parent reference
        EntityReference parent = documentReference.getParent().getParent();
        // We might be at the wiki level
        if (parent instanceof SpaceReference) {
            DocumentReference parentReference = new DocumentReference("WebHome", (SpaceReference) parent);

            documentationBridge.setParent(documentReference, parentReference);
        } else {
            // In that case (wiki level), just set a null reference.
            documentationBridge.setParent(documentReference, null);
        }
    }
}
