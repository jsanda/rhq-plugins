/*
 * RHQ Management Platform
 * Copyright (C) 2010 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.admin.templates;

import com.smartgwt.client.data.Criteria;

import org.rhq.core.domain.authz.Permission;
import org.rhq.core.domain.criteria.MeasurementScheduleCriteria;
import org.rhq.enterprise.gui.coregui.client.PermissionsLoadedListener;
import org.rhq.enterprise.gui.coregui.client.PermissionsLoader;
import org.rhq.enterprise.gui.coregui.client.inventory.common.AbstractMeasurementScheduleListView;

import java.util.EnumSet;
import java.util.Set;

/**
 * A view for viewing and updating the default metric schedules ("metric templates") for a particular ResourceType.
 *
 * @author Ian Springer
 */
public class TemplateSchedulesView extends AbstractMeasurementScheduleListView {

    private static final String TITLE = MSG.view_admin_measTemplates_title();
    private static final String[] EXCLUDED_FIELD_NAMES = new String[] { MeasurementScheduleCriteria.FILTER_FIELD_RESOURCE_TYPE_ID };

    private boolean updateExistingSchedules = true;
    private Set<Permission> globalPermissions;

    public TemplateSchedulesView(String locatorId, int resourceTypeId) {
        super(locatorId, TITLE, new TemplateSchedulesDataSource(resourceTypeId), createCriteria(resourceTypeId),
            EXCLUDED_FIELD_NAMES);

        this.globalPermissions = EnumSet.noneOf(Permission.class);
        loadGlobalPermissions();
    }

    public boolean hasManageMeasurementsPermission() {
        loadGlobalPermissions();
        return globalPermissions.contains(Permission.MANAGE_INVENTORY);
    }

    private void loadGlobalPermissions() {
        new PermissionsLoader().loadExplicitGlobalPermissions(new PermissionsLoadedListener() {
            public void onPermissionsLoaded(Set<Permission> permissions) {
                globalPermissions = permissions;
            }
        });
    }

    private static Criteria createCriteria(int resourceTypeId) {
        Criteria criteria = new Criteria();
        criteria.addCriteria(MeasurementScheduleCriteria.FILTER_FIELD_RESOURCE_TYPE_ID, resourceTypeId);
        return criteria;
    }

    @Override
    protected void configureTable() {
        super.configureTable();

        addExtraWidget(new UpdateExistingSchedulesWidget(this), true);
    }

    public boolean isUpdateExistingSchedules() {
        return updateExistingSchedules;
    }

    public void setUpdateExistingSchedules(boolean updateExistingSchedules) {
        this.updateExistingSchedules = updateExistingSchedules;
    }

}
