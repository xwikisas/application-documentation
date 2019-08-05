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
package com.xwiki.documentation;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.stability.Unstable;

/**
 * Manager for section ordering operations.
 *
 * @version $Id$
 * @since 1.0
 */
@Role
@Unstable
public interface SectionOrderingManager
{
    /**
     * Will compute the previous and the next section for the given {@link DocumentReference}. Once the sections
     * are computed, they will be stored in the document SectionClass.
     *
     * @param documentReference the document to use
     * @throws DocumentationException if the computation failed
     */
    void computePreviousAndNextSections(DocumentReference documentReference) throws DocumentationException;

    /**
     * Will compute the {@link DocumentReference} of the previous section of the given document.
     *
     * @param documentReference the document to use.
     * @return the previous section, can be null if the given document is the first section of the documentation
     * @throws DocumentationException if the computation failed
     */
    DocumentReference computePreviousSection(DocumentReference documentReference) throws DocumentationException;

    /**
     * Will compute the {@link DocumentReference} of the next section of the given document.
     *
     * @param documentReference the document to use.
     * @return the next section, can be null if the given document is the last section of the documentation
     * @throws DocumentationException if the computation failed
     */
    DocumentReference computeNextSection(DocumentReference documentReference) throws DocumentationException;
}
