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
 * Manager for the section numbering.
 *
 * @since 1.0
 * @version $Id$
 */
@Role
public interface SectionNumberingManager
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
     * @throws DocumentationException if an error occurred during the computing of the numbering or if the document
     * don't have any numbering.
     */
    String getFullNumbering(DocumentReference documentReference) throws DocumentationException;

    /**
     * Extrapolates the full numbering of a new section based on its previous sibling reference and
     * its parent section reference.
     *
     * @param previousSiblingReference the previous sibling of the document. Can be null in teh case where the
     * current document is the first child of the parent.
     * @param parentSectionReference the parent document.
     * @return the computed numbering
     * @throws DocumentationException if an error happens
     */
    String getFullNumbering(DocumentReference previousSiblingReference, DocumentReference parentSectionReference)
        throws DocumentationException;

    /**
     * Extrapolates the numbering of a new section based on its previous sibling reference.
     * Most of the time, the result will be the numbering of the previous sibling + 1.
     * If the previous sibling appears to be null, the method will necessarily return 1.
     *
     * @param previousSiblingReference the previous sibling of the document that we are trying to create.
     * @return the computed number
     * @throws DocumentationException if an error happens
     */
    long getNumberingFromSibling(DocumentReference previousSiblingReference) throws DocumentationException;
}
