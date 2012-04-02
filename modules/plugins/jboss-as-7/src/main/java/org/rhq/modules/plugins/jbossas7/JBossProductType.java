/*
 * RHQ Management Platform
 * Copyright (C) 2012 Red Hat, Inc.
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
package org.rhq.modules.plugins.jbossas7;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * A product based on JBoss 7.x.
 *
 * @author Ian Springer
 */
public enum JBossProductType {

    AS("AS", "JBoss AS 7", "JBoss Application Server 7"),
    EAP("EAP", "JBoss EAP 6", "JBoss Enterprise Application Platform 6"),
    EDG("EDG", "JBoss EDG 6", "JBoss Enterprise Data Grid 6"),
    EPP("EPP", "JBoss EAP 6", "JBoss Enterprise Portal Platform 6"),
//    EWP("EWP", "JBoss EWP 6", "JBoss Enterprise Web Platform 6"),
    SOA("SOA-P", "JBoss SOA-P 6", "JBoss Enterprise SOA Platform (ESB)");

    public final String SHORT_NAME;
    public final String NAME;
    public final String FULL_NAME;

    JBossProductType(String shortName, String name, String fullName) {
        this.SHORT_NAME = shortName;
        this.NAME = name;
        this.FULL_NAME = fullName;
    }

    /**
     * Determines the product type of a JBoss product installation.
     *
     * @param homeDir the JBoss product installation directory (e.g. /opt/jboss-as-7.1.1.Final) 
     *
     * @return the product type
     */
    public static JBossProductType determineJBossProductType(File homeDir) {
        try {
            return determineJBossProductTypeViaProductConfFile(homeDir);
        } catch (Exception e) {
            // TODO: Log an error.
            return determineJBossProductTypeViaHomeDirName(homeDir);
        }
    }

    private static JBossProductType determineJBossProductTypeViaProductConfFile(File homeDir) throws Exception {
        JBossProductType productType;
        File productConfFile = new File(homeDir, "bin/product.conf");
        if (productConfFile.exists()) {
            // It's some product (i.e. not community AS).
            Properties productConfProps = new Properties();
            FileInputStream inputStream = new FileInputStream(productConfFile);
            try {
                productConfProps.load(inputStream);
            } catch (IOException e) {
                throw new Exception("Failed to parse " + productConfFile + ".", e);
            } finally {
                inputStream.close();
            }
            String slot = productConfProps.getProperty("slot", "").trim();
            if (slot.isEmpty()) {
                throw new Exception("'slot' property not found in " + productConfFile + ".");
            }
            if (slot.equals("eap")) {
                productType = JBossProductType.EAP;
            } else if (slot.equals("edg")) {
                productType = JBossProductType.EDG;
            } else if (slot.equals("epp")) {
                productType = JBossProductType.EPP;
            } else if (slot.equals("soa-p")) {
                productType = JBossProductType.SOA;
            } else {
                throw new RuntimeException("Unknown product type: " + slot);
            }
        } else {
            productType = JBossProductType.AS;
        }

        return productType;
    }

    private static JBossProductType determineJBossProductTypeViaHomeDirName(File homeDir) {
        JBossProductType productType;
        String homeDirName = homeDir.getName();
        if (homeDirName.contains("-as-")) {
            productType = JBossProductType.AS;
        } else if (homeDirName.contains("-eap-")) {
            productType = JBossProductType.EAP;
        } else if (homeDirName.contains("-edg-")) {
            productType = JBossProductType.EDG;
        } else if (homeDirName.contains("-epp-")) {
             productType = JBossProductType.EPP;
        } else if (homeDirName.contains("soa-p-")) {
             productType = JBossProductType.SOA;
        } else {
             throw new RuntimeException("Failed to determine product type for JBoss product installed at [" + homeDir + "].");
        }

        return productType;
    }
    
    @Override
    public String toString() {
        return this.NAME;
    }

}