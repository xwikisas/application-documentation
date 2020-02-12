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

/**
 * Defines genereric helpers around sections.
 *
 * @version $Id$
 * @since 1.1-SNAPSHOT
 */
@Role
public interface SectionManager
{
    /**
     * @param documentReference the document to inspect
     * @return true if the given document has a SectionClass
     * @throws DocumentationException if an error happens
     */
    boolean isSection(DocumentReference documentReference) throws DocumentationException;

    /**
     * Checks whether the given reference is to be included in the export or not, optionally by checking recursively
     * the parent spaces if parameter withUpperSpaceCheck is true. A page is excluded either if has a property
     * "includeInExports" set to "false" or it is part of a space that is excluded.
     *
     * @param documentReference the document to inspect
     * @param withUpperSpaceCheck true if the upper spaces should be checked for inclusion
     * @return true if the given section should be included in exports.
     * @throws DocumentationException if an error happens
     */
    boolean isIncludedInExports(DocumentReference documentReference, boolean withUpperSpaceCheck)
            throws DocumentationException;


    /**
     * Returns a document reference pointing at the previous or next section, taking into account the fact that not
     * all sections are included in the export.
     *
     * @param reference a DocumentReference
     * @param propertyName either 'previousSection' or 'nextSection'
     * @return a reference to the previous or next section
     * @throws DocumentationException in case an error occurs
     */

    DocumentReference getPreviousOrNextIncludedSection(DocumentReference reference, String propertyName)
            throws DocumentationException;

}
