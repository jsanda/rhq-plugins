/*
* Jopr Management Platform
* Copyright (C) 2005-2011 Red Hat, Inc.
* All rights reserved.
*
* This program is free software; you can redistribute it and/or modify
* it under the terms of the GNU General Public License, version 2, as
* published by the Free Software Foundation, and/or the GNU Lesser
* General Public License, version 2.1, also as published by the Free
* Software Foundation.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License and the GNU Lesser General Public License
* for more details.
*
* You should have received a copy of the GNU General Public License
* and the GNU Lesser General Public License along with this program;
* if not, write to the Free Software Foundation, Inc.,
* 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
*/
package org.rhq.plugins.jbossas5.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import org.jboss.deployers.spi.management.KnownDeploymentTypes;
import org.jboss.managed.api.ComponentType;
import org.jboss.managed.api.ManagedOperation;
import org.jboss.managed.api.ManagedParameter;
import org.jboss.managed.api.ManagedProperty;
import org.jboss.managed.api.annotation.ViewUse;
import org.jboss.metatype.api.types.MapCompositeMetaType;
import org.jboss.metatype.api.types.MetaType;
import org.jboss.metatype.api.types.SimpleMetaType;
import org.jboss.metatype.api.values.MetaValue;

import org.rhq.core.domain.configuration.Configuration;
import org.rhq.core.domain.configuration.Property;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinition;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionList;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionMap;
import org.rhq.core.domain.configuration.definition.PropertyDefinitionSimple;
import org.rhq.core.domain.configuration.definition.PropertySimpleType;
import org.rhq.core.domain.measurement.MeasurementDefinition;
import org.rhq.core.domain.measurement.MeasurementReport;
import org.rhq.core.domain.measurement.MeasurementScheduleRequest;
import org.rhq.core.domain.operation.OperationDefinition;
import org.rhq.core.domain.resource.ResourceType;
import org.rhq.plugins.jbossas5.AbstractManagedDeploymentComponent;
import org.rhq.plugins.jbossas5.ManagedComponentComponent;
import org.rhq.plugins.jbossas5.adapter.api.MeasurementAdapter;
import org.rhq.plugins.jbossas5.adapter.api.MeasurementAdapterFactory;
import org.rhq.plugins.jbossas5.adapter.api.PropertyAdapter;
import org.rhq.plugins.jbossas5.adapter.api.PropertyAdapterFactory;

/**
 * Utility class to convert some basic Profile Service objects to JON objects, and some basic manipulation and data
 * gathering of Profile Service objects.
 * <p/>
 * This should not include converting between Property objects and ManagedProperties. Those conversions should be in the
 * corresponding Adapter classes.
 *
 * @author Ian Springer
 * @author Mark Spritzler
 */
public class ConversionUtils {

    private static final Log LOG = LogFactory.getLog(ConversionUtils.class);

    private static final Map<String, ComponentType> COMPONENT_TYPE_CACHE = new HashMap<String, ComponentType>();
    private static final Map<String, KnownDeploymentTypes> DEPLOYMENT_TYPE_CACHE = new HashMap<String, KnownDeploymentTypes>();
    private static final Map<String, Configuration> DEFAULT_PLUGIN_CONFIG_CACHE = new HashMap<String, Configuration>();

    public static ComponentType getComponentType(@NotNull ResourceType resourceType) {
        String resourceTypeName = resourceType.getName();
        if (COMPONENT_TYPE_CACHE.containsKey(resourceTypeName))
            return COMPONENT_TYPE_CACHE.get(resourceTypeName);
        Configuration defaultPluginConfig = getDefaultPluginConfiguration(resourceType);
        String type = defaultPluginConfig.getSimpleValue(ManagedComponentComponent.Config.COMPONENT_TYPE, null);
        if (type == null || type.equals(""))
            throw new IllegalStateException("Required plugin configuration property '"
                + ManagedComponentComponent.Config.COMPONENT_TYPE + "' is not defined in default template.");
        String subtype = defaultPluginConfig.getSimpleValue(ManagedComponentComponent.Config.COMPONENT_SUBTYPE, null);
        if (subtype == null || subtype.equals(""))
            throw new IllegalStateException("Required plugin configuration property '"
                + ManagedComponentComponent.Config.COMPONENT_SUBTYPE + "' is not defined in default template.");
        ComponentType componentType = new ComponentType(type, subtype);
        COMPONENT_TYPE_CACHE.put(resourceTypeName, componentType);
        return componentType;
    }

    public static KnownDeploymentTypes getDeploymentType(@NotNull ResourceType resourceType) {
        String resourceTypeName = resourceType.getName();
        if (DEPLOYMENT_TYPE_CACHE.containsKey(resourceTypeName))
            return DEPLOYMENT_TYPE_CACHE.get(resourceTypeName);
        Configuration defaultPluginConfig = getDefaultPluginConfiguration(resourceType);
        String typeName = defaultPluginConfig.getSimpleValue(
            AbstractManagedDeploymentComponent.DEPLOYMENT_TYPE_NAME_PROPERTY, null);
        if (typeName == null || typeName.equals(""))
            throw new IllegalStateException("Required plugin configuration property '"
                + ManagedComponentComponent.Config.COMPONENT_TYPE + "' is not defined in default template.");
        KnownDeploymentTypes deploymentType = KnownDeploymentTypes.valueOf(typeName);
        DEPLOYMENT_TYPE_CACHE.put(resourceTypeName, deploymentType);
        return deploymentType;
    }

    public static Configuration convertManagedObjectToConfiguration(Map<String, ManagedProperty> managedProperties,
        Map<String, PropertySimple> customProps, ResourceType resourceType) {
        Configuration config = new Configuration();
        ConfigurationDefinition configDef = resourceType.getResourceConfigurationDefinition();
        Map<String, PropertyDefinition> propDefs = configDef.getPropertyDefinitions();
        Set<String> propNames = managedProperties.keySet();
        for (String propName : propNames) {
            PropertyDefinition propertyDefinition = propDefs.get(propName);
            ManagedProperty managedProperty = managedProperties.get(propName);
            if (propertyDefinition == null) {
                if (!managedProperty.hasViewUse(ViewUse.STATISTIC))
                    LOG.debug(resourceType + " does not define a property corresponding to ManagedProperty '"
                        + propName + "'.");
                continue;
            }
            if (managedProperty == null) {
                // This should never happen, but don't let it blow us up.
                LOG.error("ManagedProperty '" + propName + "' has a null value in the ManagedProperties Map.");
                continue;
            }
            MetaValue metaValue = managedProperty.getValue();
            if (managedProperty.isRemoved() || metaValue == null) {
                // Don't even add a Property to the Configuration if the ManagedProperty is flagged as removed or has a
                // null value.
                continue;
            }
            PropertySimple customProp = customProps.get(propName);
            PropertyAdapter propertyAdapter = PropertyAdapterFactory.getCustomPropertyAdapter(customProp);
            if (propertyAdapter == null) {
                propertyAdapter = PropertyAdapterFactory.getPropertyAdapter(metaValue);
            }
            if (propertyAdapter == null) {
                LOG.error("Unable to find a PropertyAdapter for ManagedProperty '" + propName + "' with MetaType ["
                    + metaValue.getMetaType() + "] for ResourceType '" + resourceType.getName() + "'.");
                continue;
            }
            Property property;
            try {
                property = propertyAdapter.convertToProperty(metaValue, propertyDefinition);
            } catch (RuntimeException e) {
                throw new RuntimeException("Failed to convert managed property " + managedProperty
                    + " to RHQ property of type " + propertyDefinition + ".", e);
            }
            config.put(property);
        }
        return config;
    }

    public static void convertConfigurationToManagedProperties(Map<String, ManagedProperty> managedProperties,
        Configuration configuration, ResourceType resourceType, Map<String, PropertySimple> customProps) {
        ConfigurationDefinition configDefinition = resourceType.getResourceConfigurationDefinition();
        Set<String> missingManagedPropertyNames = new HashSet<String>();
        for (Property property : configuration.getProperties()) {
            String propertyName = property.getName();
            ManagedProperty managedProperty = managedProperties.get(propertyName);
            PropertyDefinition propertyDefinition = configDefinition.get(propertyName);
            if (managedProperty == null) {
                // NOTE: We expect the Profile Service to always return templates that contain *all* ManagedProperties
                //       that are defined for the ComponentType, so this is considered an error. We could build a
                //       ManagedProperty from scratch based on only a PropertyDefinition anyway, since a propDef could
                //       map to multiple different types of MetaValues (e.g. a PropertyList could potentially map to
                //       either an ArrayValue or a CollectionValue).
                missingManagedPropertyNames.add(propertyName);
            } else {
                populateManagedPropertyFromProperty(property, propertyDefinition, managedProperty,
                    customProps.get(propertyName));
            }
            if (!missingManagedPropertyNames.isEmpty())
                throw new IllegalStateException("***** The following properties are defined in this plugin's "
                    + "descriptor but have no corresponding ManagedProperties: " + missingManagedPropertyNames);
        }
        return;
    }

    private static Configuration getDefaultPluginConfiguration(ResourceType resourceType) {
        Configuration defaultPluginConfig;
        if (DEFAULT_PLUGIN_CONFIG_CACHE.containsKey(resourceType.getName()))
            defaultPluginConfig = DEFAULT_PLUGIN_CONFIG_CACHE.get(resourceType.getName());
        else {
            defaultPluginConfig = ResourceTypeUtils.getDefaultPluginConfiguration(resourceType);
            DEFAULT_PLUGIN_CONFIG_CACHE.put(resourceType.getName(), defaultPluginConfig);
        }
        return defaultPluginConfig;
    }

    private static void populateManagedPropertyFromProperty(Property property, PropertyDefinition propertyDefinition,
        @NotNull ManagedProperty managedProperty, @Nullable PropertySimple customProperty) {
        // See if there is a custom adapter defined for this property.
        PropertyAdapter propertyAdapter = PropertyAdapterFactory.getCustomPropertyAdapter(customProperty);

        MetaValue metaValue = managedProperty.getValue();
        if (metaValue != null) {
            LOG.trace("Populating existing MetaValue of type " + metaValue.getMetaType() + " from RHQ property "
                + property + " with definition " + propertyDefinition + "...");
            if (propertyAdapter == null) {
                propertyAdapter = PropertyAdapterFactory.getPropertyAdapter(metaValue);
            }
            propertyAdapter.populateMetaValueFromProperty(property, metaValue, propertyDefinition);
        } else {
            MetaType metaType = managedProperty.getMetaType();
            if (propertyAdapter == null) {
                propertyAdapter = PropertyAdapterFactory.getPropertyAdapter(metaType);
            }
            LOG.trace("Converting property " + property + " with definition " + propertyDefinition
                + " to MetaValue of type " + metaType + "...");
            metaValue = propertyAdapter.convertToMetaValue(property, propertyDefinition, metaType);
            managedProperty.setValue(metaValue);
        }
    }

    public static MetaType convertPropertyDefinitionToMetaType(PropertyDefinition propDef) {
        MetaType memberMetaType;
        if (propDef instanceof PropertyDefinitionSimple) {
            PropertySimpleType propSimpleType = ((PropertyDefinitionSimple) propDef).getType();
            memberMetaType = convertPropertySimpleTypeToSimpleMetaType(propSimpleType);
        } else if (propDef instanceof PropertyDefinitionList) {
            // TODO (very low priority, since lists of lists are not going to be at all common)
            memberMetaType = null;
        } else if (propDef instanceof PropertyDefinitionMap) {
            List<PropertyDefinition> memberPropDefs = ((PropertyDefinitionMap) propDef).getPropertyDefinitions();
            if (memberPropDefs.isEmpty())
                throw new IllegalStateException("PropertyDefinitionMap doesn't contain any member PropertyDefinitions.");
            // NOTE: We assume member prop defs are all of the same type, since for MapCompositeMetaTypes, they have to be.
            PropertyDefinition mapMemberPropDef = memberPropDefs.get(0);
            MetaType mapMemberMetaType = convertPropertyDefinitionToMetaType(mapMemberPropDef);
            memberMetaType = new MapCompositeMetaType(mapMemberMetaType);
        } else {
            throw new IllegalStateException("List member PropertyDefinition has unknown type: "
                + propDef.getClass().getName());
        }
        return memberMetaType;
    }

    private static MetaType convertPropertySimpleTypeToSimpleMetaType(PropertySimpleType memberSimpleType) {
        MetaType memberMetaType;
        Class memberClass;
        switch (memberSimpleType) {
        case BOOLEAN:
            memberClass = Boolean.class;
            break;
        case INTEGER:
            memberClass = Integer.class;
            break;
        case LONG:
            memberClass = Long.class;
            break;
        case FLOAT:
            memberClass = Float.class;
            break;
        case DOUBLE:
            memberClass = Double.class;
            break;
        default:
            memberClass = String.class;
            break;
        }
        memberMetaType = SimpleMetaType.resolve(memberClass.getName());
        return memberMetaType;
    }

    /**
     * Takes the Configuration parameter object and converts it into a MetaValue array, which can them be passed in with
     * the invoke method to the ProfileService to fire the operation of a resource.
     *
     * @param managedOperation    Operation that will be fired and stores the parameter types for the operation
     * @param parameters          set of Parameter Values that the OperationFacet sent to the Component
     * @param operationDefinition the RHQ operation definition
     * @return MetaValue[] array of MetaValues representing the parameters; if there are no parameters, an empty array
     *         will be returned
     */
    @NotNull
    public static MetaValue[] convertOperationsParametersToMetaValues(@NotNull ManagedOperation managedOperation,
        @NotNull Configuration parameters, @NotNull OperationDefinition operationDefinition) {
        ConfigurationDefinition paramsConfigDef = operationDefinition.getParametersConfigurationDefinition();
        if (paramsConfigDef == null)
            return new MetaValue[0];
        ManagedParameter[] managedParams = managedOperation.getParameters(); // this is guaranteed to be non-null
        Map<String, PropertyDefinition> paramPropDefs = paramsConfigDef.getPropertyDefinitions();
        MetaValue[] paramMetaValues = new MetaValue[managedParams.length];
        for (int i = 0; i < managedParams.length; i++) {
            ManagedParameter managedParam = managedParams[i];
            String paramName = managedParam.getName();
            Property paramProp = parameters.get(paramName);
            PropertyDefinition paramPropDef = paramPropDefs.get(paramName);
            MetaType metaType = managedParam.getMetaType();
            PropertyAdapter propertyAdapter = PropertyAdapterFactory.getPropertyAdapter(metaType);
            LOG.trace("Converting RHQ operation param property " + paramProp + " with definition " + paramPropDef
                + " to MetaValue of type " + metaType + "...");
            MetaValue paramMetaValue = propertyAdapter.convertToMetaValue(paramProp, paramPropDef, metaType);
            // NOTE: There's no need to set the value on the ManagedParameter, since the invoke() API takes an array of
            //       MetaValues.
            paramMetaValues[i] = paramMetaValue;
        }
        return paramMetaValues;
    }

    public static void convertManagedOperationResults(ManagedOperation operation, MetaValue resultMetaValue,
        Configuration complexResults, OperationDefinition operationDefinition) {
        ConfigurationDefinition resultConfigDef = operationDefinition.getResultsConfigurationDefinition();
        // Don't return any results if we have no definition with which to display them
        if (resultConfigDef == null || resultConfigDef.getPropertyDefinitions().isEmpty()) {
            if (resultMetaValue != null) {
                LOG.error("Plugin error: Operation [" + operationDefinition.getName()
                    + "] is defined as returning no results, but it returned non-null results: "
                    + resultMetaValue.toString());
            }
            return;
        } else {
            Map<String, PropertyDefinition> resultPropDefs = resultConfigDef.getPropertyDefinitions();
            // There should and must be only one property definition to map to the results from the Profile Service,
            // otherwise there will be a huge mismatch.
            if (resultPropDefs.size() > 1)
                LOG.error("Operation [" + operationDefinition.getName()
                    + "] is defined with multiple result properties: " + resultPropDefs.values());

            PropertyDefinition resultPropDef = resultPropDefs.values().iterator().next();

            // Don't return any results, if the actual result object is null.
            if (resultMetaValue == null) {
                // Check if result is required or not, and if it is, log an error.
                if (resultPropDef.isRequired()) {
                    LOG.error("Plugin error: Operation [" + operationDefinition.getName()
                        + "] is defined as returning a required result, but it returned null.");
                }
                return;
            }

            MetaType resultMetaType = operation.getReturnType();
            if (!MetaTypeUtils.instanceOf(resultMetaValue, resultMetaType))
                LOG.debug("Profile Service Error: Result type (" + resultMetaType + ") of [" + operation.getName()
                    + "] ManagedOperation does not match the type of the value returned by invoke() ("
                    + resultMetaValue + ").");

            PropertyAdapter propertyAdapter = PropertyAdapterFactory.getPropertyAdapter(resultMetaValue);
            Property resultProp = propertyAdapter.convertToProperty(resultMetaValue, resultPropDef);
            complexResults.put(resultProp);
        }
    }

    public static void convertMetricValuesToMeasurement(MeasurementReport report, ManagedProperty metricProperty,
        MeasurementScheduleRequest request, ResourceType resourceType, String deploymentName) {
        String metricName = metricProperty.getName();
        MetaType type = metricProperty.getMetaType();
        MetaValue value = metricProperty.getValue();
        if (value != null) {
            MeasurementAdapter measurementAdapter = MeasurementAdapterFactory.getMeasurementPropertyAdapter(type);
            MeasurementDefinition measurementDefinition = ResourceTypeUtils.getMeasurementDefinition(resourceType,
                metricName);
            if (measurementDefinition != null) {
                measurementAdapter.setMeasurementData(report, value, request, measurementDefinition);
            }
        } else {
            LOG.debug("Unable to obtain metric data for resource: " + deploymentName + " metric: " + metricName);
        }
    }

}
