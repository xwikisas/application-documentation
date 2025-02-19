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
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;

import com.xwiki.documentation.DocumentationBridge;
import com.xwiki.documentation.DocumentationException;
import com.xwiki.documentation.SectionManager;

/**
 * This is the default implementation of {@link SectionManager}.
 *
 * @version $Id$
 * @since 1.1-SNAPSHOT
 */
@Component
@Singleton
public class DefaultSectionManager implements SectionManager
{
    @Inject
    private QueryManager queryManager;

    @Inject
    @Named("local")
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Inject
    private DocumentationBridge documentationBridge;

    @Override
    public boolean isSection(DocumentReference documentReference) throws DocumentationException
    {
        try {
            String hqlQuery = "select sectionObject.name from BaseObject sectionObject "
                + "where sectionObject.className = 'Documentation.Code.SectionClass' "
                + "and sectionObject.name = :docName";
            return queryManager.createQuery(hqlQuery, Query.HQL)
                .bindValue("docName", entityReferenceSerializer.serialize(documentReference))
                .execute().size() > 0;
        } catch (QueryException e) {
            throw new DocumentationException(
                String.format("Failed to verify if document [%s] is a section.", documentReference), e);
        }
    }

    @Override
    public boolean isIncludedInExports(DocumentReference sectionReference, boolean withUpperSpacesCheck)
        throws DocumentationException
    {
        if (isSection(sectionReference)) {
            if (!documentationBridge.getIsIncludedInExports(sectionReference, withUpperSpacesCheck)) {
                return false;
            } else {
                DocumentReference parentSectionReference = documentationBridge.getParentSection(sectionReference);
                return (parentSectionReference != null)
                    ? isIncludedInExports(parentSectionReference, withUpperSpacesCheck) : true;
            }
        } else {
            return true;
        }
    }

    @Override
    public DocumentReference getPreviousOrNextIncludedSection(DocumentReference reference, String propertyName)
        throws DocumentationException
    {
        return documentationBridge.getPreviousOrNextIncludedSection(reference, propertyName);
    }
}
