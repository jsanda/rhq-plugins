/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
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
package org.rhq.core.domain.plugin;

import java.io.ByteArrayInputStream;
import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.PrePersist;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.jetbrains.annotations.NotNull;

import org.rhq.core.domain.util.MD5Generator;

/**
 * An agent plugin.
 * 
 * This object contains information about the plugin jar itself (e.g. its name and MD5).
 * It may also contain the jar contents ({@link #getContent()}).
 */
@Entity
@NamedQueries( { @NamedQuery(name = Plugin.QUERY_FIND_BY_NAME, query = "SELECT p FROM Plugin AS p WHERE p.name=:name"),
    @NamedQuery(name = Plugin.QUERY_FIND_BY_PATH, query = "SELECT p FROM Plugin AS p WHERE p.path=:path"),
    @NamedQuery(name = Plugin.QUERY_FIND_ALL, query = "SELECT p FROM Plugin AS p") })
@SequenceGenerator(name = "SEQ", sequenceName = "RHQ_PLUGIN_ID_SEQ")
@Table(name = Plugin.TABLE_NAME)
public class Plugin implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String TABLE_NAME = "RHQ_PLUGIN";

    public static final String QUERY_FIND_ALL = "Plugin.findAll";
    public static final String QUERY_FIND_BY_NAME = "Plugin.findByName";
    public static final String QUERY_FIND_BY_PATH = "Plugin.findByPath";

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ")
    @Id
    private int id;

    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "DISPLAY_NAME", nullable = false)
    private String displayName;

    @Column(name = "DESCRIPTION", nullable = true)
    private String description;

    @Column(name = "ENABLED", nullable = false)
    private boolean enabled = true;

    @Column(name = "HELP", nullable = true)
    private String help;

    @Column(name = "VERSION", nullable = true)
    private String version;

    @Column(name = "PATH", nullable = false)
    private String path;

    @Column(name = "MD5", nullable = false)
    private String md5;

    @Column(name = "CTIME", nullable = false)
    private long ctime = System.currentTimeMillis();

    @Column(name = "CONTENT", nullable = true)
    private byte[] content;

    protected Plugin() {
    }

    /**
     * Constructor for {@link Plugin}.
     *
     * @param name the logical name of the plugin
     * @param path the actual filename of the plugin jar (see {@link #getPath()})
     */
    public Plugin(@NotNull String name, String path) {
        this.name = name;
        this.path = path;
    }

    /**
     * Constructor for {@link Plugin}.
     * Note that this allows you to provide an MD5 without providing the plugin's
     * actual content. If you wish to persist this entity in the database, you should
     * either provide the {@link #setContent(byte[]) content} or update the entity
     * later by streaming the file content to the content column.
     *
     * @param name the logical name of the plugin
     * @param path the actual filename of the plugin jar (see {@link #getPath()})
     * @param md5  the MD5 hash string of the plugin jar contents
     */
    public Plugin(String name, String path, String md5) {
        this.name = name;
        this.path = path;
        this.md5 = md5;
    }

    /**
     * Constructor for {@link Plugin}.
     *
     * @param name the logical name of the plugin
     * @param path the actual filename of the plugin jar (see {@link #getPath()})
     * @param content the actual jar file contents (the MD5 hash string will be generated from this)
     */
    public Plugin(String name, String path, byte[] content) {
        this.name = name;
        this.path = path;
        this.content = content;

        try {
            ByteArrayInputStream stream = new ByteArrayInputStream(content);
            this.md5 = MD5Generator.getDigestString(stream);
        } catch (Exception e) {
            throw new RuntimeException("Cannot determine plugin's MD5!", e);
        }
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCtime(long ctime) {
        this.ctime = ctime;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getHelp() {
        return help;
    }

    public void setHelp(String help) {
        this.help = help;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public String getMD5() {
        return getMd5();
    }

    public void setMD5(String md5) {
        setMd5(md5);
    }

    /**
     * Returns the actual name of the plugin jar. This is not the absolute path, in fact, it does not include any
     * directory paths. It is strictly the name of the plugin jar as found on the file system (aka the filename).
     *
     * @return plugin filename
     */
    public String getPath() {
        return this.path;
    }

    /**
     * Ensure that the path being set does not include any directory names. The plugin path is the filename. See
     * {@link #getPath()}.
     *
     * @param path the filename of the plugin, not including directory names
     */
    public void setPath(String path) {
        this.path = path;
    }

    public long getCtime() {
        return this.ctime;
    }

    /**
     * Returns the actual content of the plugin file. Be careful calling this
     * in an entity context - the entire plugin file content will be loaded in
     * memory (which may trigger an OutOfMemoryError if the file is very large).
     * 
     * @return the content of the plugin file
     */
    public byte[] getContent() {
        return this.content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    @PrePersist
    void onPersist() {
        this.ctime = System.currentTimeMillis();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if ((obj == null) || !(obj instanceof Plugin)) {
            return false;
        }

        Plugin that = (Plugin) obj;
        return name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return "Plugin[id=" + id + ", name=" + name + ", md5=" + md5 + "]";
    }
}