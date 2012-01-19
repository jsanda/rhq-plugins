/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.enterprise.server.resource;

import javax.ejb.Remote;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.criteria.ResourceTypeCriteria;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.core.domain.util.PageList;
import org.rhq.enterprise.server.system.ServerVersion;

@SOAPBinding(style = SOAPBinding.Style.DOCUMENT)
@WebService(targetNamespace = ServerVersion.namespace)
@Remote
public interface ResourceTypeManagerRemote {

    @WebMethod
    ResourceType getResourceTypeById(//
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "resourceTypeId") int resourceTypeId) //
        throws ResourceTypeNotFoundException;

    /**
     * @param  subject
     * @param  name
     * @param  plugin
     *
     * @return the resource type by name and plugin or null if the type is not found
     */
    @WebMethod
    ResourceType getResourceTypeByNameAndPlugin(//
        @WebParam(name = "subject") Subject subject, //        
        @WebParam(name = "name") String name, //
        @WebParam(name = "plugin") String plugin);

    @WebMethod
    PageList<ResourceType> findResourceTypesByCriteria(//
        @WebParam(name = "subject") Subject subject, //
        @WebParam(name = "criteria") ResourceTypeCriteria criteria);
}