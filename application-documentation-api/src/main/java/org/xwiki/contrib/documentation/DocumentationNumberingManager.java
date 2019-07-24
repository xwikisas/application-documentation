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
package org.xwiki.contrib.documentation;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;

/**
 * Manager for the documentation numbering.
 *
 * @since 1.0
 * @version $Id$
 */
@Role
public interface DocumentationNumberingManager
{
    /**
     * @param documentReference the document to inspect
     * @return true if the given document has a SectionClass
     * @throws DocumentationException if an error happens
     */
    boolean isSection(DocumentReference documentReference) throws DocumentationException;

    /**
     * Get the full numbering of the given document.
     * @param documentReference the document to use
     * @return the full numbering, something like 1.2.1.3.
     * @throws DocumentationException if an error occured during the computing of the numbering or if the document
     * don't have any numbering.
     */
    String getFullNumbering(DocumentReference documentReference) throws DocumentationException;

    /**
     * Will compute the previous and the next document for the given {@link DocumentReference}. Once the siblings
     * are computed, they will be stored in the document SectionClass.
     *
     * @param documentReference the document to use
     * @throws DocumentationException if the computation failed
     */
    void computeSiblings(DocumentReference documentReference) throws DocumentationException;
}
