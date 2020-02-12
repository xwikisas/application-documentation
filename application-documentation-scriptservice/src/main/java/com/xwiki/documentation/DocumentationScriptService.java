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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.script.service.ScriptService;
import org.xwiki.stability.Unstable;

/**
 * This is the script service for the Documentation application.
 *
 * @version $Id$
 * @since 1.0
 */
@Component
@Singleton
@Unstable
@Named("documentation")
public class DocumentationScriptService implements ScriptService
{
    @Inject
    private SectionNumberingManager sectionNumberingManager;

    @Inject
    private SectionOrderingManager sectionOrderingManager;

    @Inject
    private SectionParentManager sectionParentManager;

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    private SectionManager sectionManager;

    /**
     * @return the {@link SectionNumberingManager}
     */
    public SectionNumberingManager getNumberingManager()
    {
        return this.sectionNumberingManager;
    }

    /**
     * @return the {@link SectionOrderingManager}
     */
    public SectionOrderingManager getOrderingManager()
    {
        return this.sectionOrderingManager;
    }

    /**
     * @return the {@link SectionParentManager}
     */
    public SectionParentManager getParentManager()
    {
        return this.sectionParentManager;
    }

    /**
     * @return wether the current document is a section
     * @throws DocumentationException if an error happens
     */
    public boolean isSection() throws DocumentationException
    {
        return sectionManager.isSection(documentAccessBridge.getCurrentDocumentReference());
    }

    /**
     * @return the full numbering of the current document
     * @throws DocumentationException if an error happens
     */
    public String getFullNumbering() throws DocumentationException
    {
        return sectionNumberingManager.getFullNumbering(documentAccessBridge.getCurrentDocumentReference());
    }

    /**
     * See {@link SectionManager#getPreviousOrNextIncludedSection(DocumentReference, String)}.
     *
     * @param reference the reference of the current document
     * @param propertyName the name of the property that we should check
     * @return the reference to the previous or next document
     * @throws DocumentationException if an error happened
     */
    public DocumentReference getPreviousOrNextIncludedSection(DocumentReference reference, String propertyName)
            throws DocumentationException
    {
        return sectionManager.getPreviousOrNextIncludedSection(reference, propertyName);
    }

    /**
     * @return true if the current document is a section that should be included in exports, false otherwise
     * @throws DocumentationException if an error happens
     */
    public boolean isIncludedInExports() throws DocumentationException
    {
        return sectionManager.isIncludedInExports(documentAccessBridge.getCurrentDocumentReference(), false);
    }

    /**
     * See {@link SectionManager#isIncludedInExports}.
     *
     * @param reference a DocumentReference
     * @param withUpperSpaceCheck true if the parent spaces should be checked for inclusion
     * @return true if the given reference is configured as included in the export
     * @throws Exception in case an error occurs
     */

    public boolean isIncludedInExports(DocumentReference reference, boolean withUpperSpaceCheck) throws DocumentationException
    {
        return sectionManager.isIncludedInExports(reference, withUpperSpaceCheck);
    }


    /**
     * @throws DocumentationException if an error happens
     */
    public void computePreviousAndNextSections() throws DocumentationException
    {
        sectionOrderingManager.computePreviousAndNextSections(documentAccessBridge.getCurrentDocumentReference());
    }

    /**
     * @throws DocumentationException if an error happens
     */
    public void computeParentSection() throws DocumentationException
    {
        sectionParentManager.computeParentSection(documentAccessBridge.getCurrentDocumentReference());
    }
}
