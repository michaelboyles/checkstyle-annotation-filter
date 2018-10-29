////////////////////////////////////////////////////////////////////////////////
// Copyright (C) 2001-2018 Checkstyle authors
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
////////////////////////////////////////////////////////////////////////////////

package uk.co.michaelboyles.checkstyle.annotationfilter;

import com.puppycrawl.tools.checkstyle.api.AuditEvent;
import com.puppycrawl.tools.checkstyle.api.AutomaticBean;
import com.puppycrawl.tools.checkstyle.api.Filter;

/**
 * Filters out any checks that were contained within the scope of classes/methods/fields annotated with the annotations
 * given to AnnotationCheck.
 */
public class AnnotationFilter extends AutomaticBean implements Filter
{
    @Override
    protected void finishLocalSetup()
    {
        // No-op
    }

    @Override
    public boolean accept(AuditEvent event)
    {
        return !AnnotationCheck.isSuppressed(event);
    }
}