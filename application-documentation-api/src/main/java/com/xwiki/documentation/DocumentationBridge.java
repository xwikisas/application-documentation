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
 * Old core bridge for operations related to documentation pages.
 *
 * @version $Id$
 * @since 1.0
 */
@Role
@Unstable
public interface DocumentationBridge
{
    /**
     * Get the numbering of the given document.
     *
     * @param documentReference the document to use.
     * @return the numbering
     * @throws DocumentationException if something goes wrong (ie : the document has no SectionClass, for example)
     */
    long getNumbering(DocumentReference documentReference) throws DocumentationException;

    /**
     * Set the numbering of the given document.
     *
     * @param documentReference the document to use.
     * @param numbering the new numbering
     * @throws DocumentationException if something goes wrong (ie : the document has no SectionClass, for example)
     */
    void setNumbering(DocumentReference documentReference, long numbering) throws DocumentationException;

    /**
     * Will set the previous / next sections of the given document to new values. If the sections are null, then the
     * property will be set as blank.
     *
     * @param documentReference the document to update
     * @param previousSection the previous section
     * @param nextSection the next section
     * @throws DocumentationException if an error happens
     */
    void setPreviousAndNextSections(DocumentReference documentReference, DocumentReference previousSection,
        DocumentReference nextSection) throws DocumentationException;

    /**
     * Will set the previous section of the given document to a new value.
     *
     * @param documentReference the document to update
     * @param previousSection the previous section
     * @throws DocumentationException if an error happens
     */
    void setPreviousSection(DocumentReference documentReference, DocumentReference previousSection)
        throws DocumentationException;

    /**
     * Will set the next section of the given document to a new value.
     *
     * @param documentReference the document to update
     * @param nextSection the next section
     * @throws DocumentationException if an error happens
     */
    void setNextSection(DocumentReference documentReference, DocumentReference nextSection)
        throws DocumentationException;

    /**
     * Will set the parent section of the given document to the given parentSection. If the parentSection is null, then
     * the property will be set as blank.
     *
     * @param documentReference the document to use
     * @param parentSection the document parent section
     * @throws DocumentationException if an error happens
     */
    void setParent(DocumentReference documentReference, DocumentReference parentSection) throws DocumentationException;

    /**
     * @param documentReference a reference to the section to inspect
     * @return a {@link DocumentReference} to the parent section of the given section or null if none is defined
     * @throws DocumentationException if an error happens
     */
    DocumentReference getParentSection(DocumentReference documentReference) throws DocumentationException;

    /**
     * @param documentReference a reference to the section to inspect
     * @param withUpperSpacesCheck true if upper spaces should be checked for inclusion
     * @return true if the section is included in exports, false otherwise
     * @throws DocumentationException if an error happens
     */
    boolean getIsIncludedInExports(DocumentReference documentReference, boolean withUpperSpacesCheck)
        throws DocumentationException;

    /**
     * Gets the next or previous included section.
     *
     * @param documentReference a section
     * @param propertyName either "previous" or "next"
     * @return a reference to the previous or next section
     * @throws DocumentationException in case an error occurs
     */
    DocumentReference getPreviousOrNextIncludedSection(DocumentReference documentReference, String propertyName)
        throws DocumentationException;
}
