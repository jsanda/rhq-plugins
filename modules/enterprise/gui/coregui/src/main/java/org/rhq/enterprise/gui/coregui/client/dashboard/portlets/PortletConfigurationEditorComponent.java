/*
 * RHQ Management Platform
 * Copyright (C) 2005-2011 Red Hat, Inc.
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
package org.rhq.enterprise.gui.coregui.client.dashboard.portlets;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import com.smartgwt.client.types.MultipleAppearance;
import com.smartgwt.client.widgets.form.fields.SelectItem;

import org.rhq.core.domain.alert.AlertPriority;
import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.util.PageOrdering;
import org.rhq.enterprise.gui.coregui.client.CoreGUI;
import org.rhq.enterprise.gui.coregui.client.ImageManager;
import org.rhq.enterprise.gui.coregui.client.Messages;
import org.rhq.enterprise.gui.coregui.client.components.measurement.CustomConfigMeasurementRangeEditor;

/** Shared portlet configuration component where initial configuration settings
 *  and widgets shared across portlet editors is defined.
 *
 * @author Simeon Pinder
 */
public class PortletConfigurationEditorComponent {

    static Messages MSG = CoreGUI.getMessages();

    public interface Constant {
        String ALERT_PRIORITY = "ALERT_PRIORITY";
        String METRIC_RANGE_ENABLE = "METRIC_RANGE_ENABLE";
        String METRIC_RANGE_BEGIN_END_FLAG = "METRIC_RANGE_BEGIN_END_FLAG";
        String METRIC_RANGE = "METRIC_RANGE";
        String METRIC_RANGE_LASTN = "METRIC_RANGE_LASTN";
        String METRIC_RANGE_LASTN_DEFAULT = String.valueOf(8);
        String METRIC_RANGE_UNIT = "METRIC_RANGE_UNIT";
        String RESULT_SEVERITY = "severities";
        String RESULT_SORT_ORDER = "RESULT_SORT_ORDER";
        String RESULT_SORT_PRIORITY = "sort.priority";
        String RESULT_COUNT = "RESULT_COUNT";
        String RESULT_COUNT_DEFAULT = "5";
        String CUSTOM_REFRESH = "CUSTOM_REFRESH";
    }

    //configuration map initialization
    public static Map<String, String> CONFIG_PROPERTY_INITIALIZATION = new HashMap<String, String>();
    static {// Key, Default value
        //alert priority, if empty initialize to "" i.e. all priorities
        CONFIG_PROPERTY_INITIALIZATION.put(Constant.ALERT_PRIORITY, "");
        //result sort order, if empty initialize to "DESC"
        CONFIG_PROPERTY_INITIALIZATION.put(Constant.RESULT_SORT_ORDER, PageOrdering.DESC.name());
        //result count, if empty initialize to 5
        CONFIG_PROPERTY_INITIALIZATION.put(Constant.RESULT_COUNT, Constant.RESULT_COUNT_DEFAULT);
        //whether to specify time range for alerts. Defaults to false
        CONFIG_PROPERTY_INITIALIZATION.put(Constant.METRIC_RANGE_ENABLE, String.valueOf(false));
        //whether Begin and End values set for time. Aka. Advanced/full range setting Defaults to false
        CONFIG_PROPERTY_INITIALIZATION.put(Constant.METRIC_RANGE_BEGIN_END_FLAG, String.valueOf(false));
        //whether in simple mode. Ex. 8 hrs. Defaults to 8
        CONFIG_PROPERTY_INITIALIZATION.put(Constant.METRIC_RANGE_LASTN, Constant.METRIC_RANGE_LASTN_DEFAULT);
    }

    /* Single select combobox for number of items to display on the dashboard
     *
     * @return Populated selectItem instance.
     */
    public static SelectItem getResultCountEditor(Configuration portletConfig) {

        final SelectItem maximumResultsComboBox = new SelectItem(Constant.RESULT_COUNT);
        maximumResultsComboBox.setTitle("Results Count");
        maximumResultsComboBox.setWrapTitle(false);
        maximumResultsComboBox.setTooltip("<nobr><b> " + "Displays N results with alerts" + "</b></nobr>");
        //spinder 9/3/10: the following is required workaround to disable editability of combobox.
        maximumResultsComboBox.setType("selection");
        //set width of dropdown display region
        maximumResultsComboBox.setWidth(100);

        //TODO: spinder 3/4/11 this is arbitrary. Get UXD input for better acceptable defaults
        int[] selectionValues = { 5, 10, 30, 100 };

        //define acceptable values for display amount
        String[] displayValues = new String[selectionValues.length];
        int i = 0;
        for (int selection : selectionValues) {
            displayValues[i++] = String.valueOf(selection);
        }
        maximumResultsComboBox.setValueMap(displayValues);
        //reload current settings if they exist, otherwise enable all.
        String currentValue = portletConfig.getSimple(Constant.RESULT_COUNT).getStringValue();
        if (currentValue.isEmpty() || currentValue.equalsIgnoreCase(Constant.RESULT_COUNT_DEFAULT)) {
            maximumResultsComboBox.setValue(Constant.RESULT_COUNT_DEFAULT);
        } else {
            maximumResultsComboBox.setValue(currentValue);
        }
        return maximumResultsComboBox;
    }

    /* Multiple select combobox for alert priorities to display on dashboard
     *
     * @return Populated selectItem instance.
     */
    public static SelectItem getAlertPriorityEditor(Configuration portletConfig) {
        SelectItem priorityFilter = new SelectItem(Constant.RESULT_SEVERITY, MSG.view_alerts_table_filter_priority());
        priorityFilter.setWrapTitle(false);
        priorityFilter.setWidth(200);
        priorityFilter.setMultiple(true);
        priorityFilter.setMultipleAppearance(MultipleAppearance.PICKLIST);

        LinkedHashMap<String, String> priorities = new LinkedHashMap<String, String>(3);
        priorities.put(AlertPriority.HIGH.name(), MSG.common_alert_high());
        priorities.put(AlertPriority.MEDIUM.name(), MSG.common_alert_medium());
        priorities.put(AlertPriority.LOW.name(), MSG.common_alert_low());
        LinkedHashMap<String, String> priorityIcons = new LinkedHashMap<String, String>(3);
        priorityIcons.put(AlertPriority.HIGH.name(), ImageManager.getAlertIcon(AlertPriority.HIGH));
        priorityIcons.put(AlertPriority.MEDIUM.name(), ImageManager.getAlertIcon(AlertPriority.MEDIUM));
        priorityIcons.put(AlertPriority.LOW.name(), ImageManager.getAlertIcon(AlertPriority.LOW));
        priorityFilter.setValueMap(priorities);
        priorityFilter.setValueIcons(priorityIcons);
        //reload current settings if they exist, otherwise enable all.
        String currentValue = portletConfig.getSimple(Constant.ALERT_PRIORITY).getStringValue();
        if (currentValue.isEmpty() || currentValue.split(",").length == AlertPriority.values().length) {
            priorityFilter.setValues(AlertPriority.HIGH.name(), AlertPriority.MEDIUM.name(), AlertPriority.LOW.name());
        } else {
            //spinder:3/4/11 doing this nonsense due to some weird smartgwt issue with SelectItem in VLayout.
            if (currentValue.equalsIgnoreCase("HIGH")) {
                priorityFilter.setValues(AlertPriority.HIGH.name());
            } else if (currentValue.equalsIgnoreCase("HIGH,MEDIUM")) {
                priorityFilter.setValues(AlertPriority.HIGH.name(), AlertPriority.MEDIUM.name());
            } else if (currentValue.equalsIgnoreCase("HIGH,LOW")) {
                priorityFilter.setValues(AlertPriority.HIGH.name(), AlertPriority.LOW.name());
            } else if (currentValue.equalsIgnoreCase("MEDIUM")) {
                priorityFilter.setValues(AlertPriority.MEDIUM.name());
            } else if (currentValue.equalsIgnoreCase("MEDIUM,LOW")) {
                priorityFilter.setValues(AlertPriority.MEDIUM.name(), AlertPriority.LOW.name());
            } else {
                priorityFilter.setValues(AlertPriority.LOW.name());
            }
        }
        return priorityFilter;
    }

    /* Single select combobox for sort order of items to display on dashboard
     *
     * @return Populated selectItem instance.
     */
    public static SelectItem getResulSortOrderEditor(Configuration portletConfig) {
        SelectItem sortPrioritySelection = new SelectItem(Constant.RESULT_SORT_PRIORITY, "Sort Order");
        sortPrioritySelection.setWrapTitle(false);
        sortPrioritySelection.setTooltip("Sets sort order for results.");
        LinkedHashMap<String, String> priorities = new LinkedHashMap<String, String>(2);
        priorities.put(PageOrdering.ASC.name(), "Ascending");
        priorities.put(PageOrdering.DESC.name(), "Descending");
        LinkedHashMap<String, String> priorityIcons = new LinkedHashMap<String, String>(2);
        priorityIcons.put(PageOrdering.ASC.name(), "ascending");
        priorityIcons.put(PageOrdering.DESC.name(), "descending");

        sortPrioritySelection.setValueMap(priorities);
        sortPrioritySelection.setValueIcons(priorityIcons);
        //TODO: spinder 3/4/11 not sure why this is necessary. [SKIN] not being interpreted.
        String skinDir = "../org.rhq.enterprise.gui.coregui.CoreGUI/sc/skins/Enterprise/images";
        sortPrioritySelection.setImageURLPrefix(skinDir + "/actions/sort_");
        sortPrioritySelection.setImageURLSuffix(".png");

        //reload current settings if they exist, otherwise enable all.
        String currentValue = portletConfig.getSimple(Constant.RESULT_SORT_ORDER).getStringValue();
        if (currentValue.isEmpty() || currentValue.equalsIgnoreCase(PageOrdering.DESC.name())) {//default to descending order
            sortPrioritySelection.setDefaultValue(PageOrdering.DESC.name());
        } else {
            sortPrioritySelection.setDefaultValue(PageOrdering.ASC.name());
        }
        return sortPrioritySelection;
    }

    /** Convenience method to construct CustomConfigMeasurementRangeEditor instances.
     *
     * @param portletConfig
     * @return
     */
    public static CustomConfigMeasurementRangeEditor getMeasurementRangeEditor(Configuration portletConfig) {
        return new CustomConfigMeasurementRangeEditor("alertTimeFrame", portletConfig);
    }

}