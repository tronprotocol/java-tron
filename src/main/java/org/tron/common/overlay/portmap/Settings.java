/**
 * UPnP PortMapper - A tool for managing port forwardings via UPnP Copyright (C) 2015 Christoph
 * Pirkl <christoph at users.sourceforge.net>
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.  If
 * not, see <http://www.gnu.org/licenses/>.
 */
package org.tron.common.overlay.portmap;

import ch.qos.logback.classic.Level;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.tron.common.overlay.portmap.model.PortMappingPreset;
import org.tron.common.overlay.portmap.router.cling.ClingRouterFactory;

public class Settings implements Serializable {

  private static final long serialVersionUID = -1349121864190290050L;

  public final static String PROPERTY_PORT_MAPPING_PRESETS = "presets";

  private List<PortMappingPreset> presets;
  private boolean useEntityEncoding;
  private String logLevel;
  private String routerFactoryClassName;

  private transient PropertyChangeSupport propertyChangeSupport;

  public Settings() {
    useEntityEncoding = true;
    logLevel = Level.INFO.toString();
    presets = new ArrayList<>();
    routerFactoryClassName = ClingRouterFactory.class.getName();
    propertyChangeSupport = new PropertyChangeSupport(this);
  }

  public void addPropertyChangeListener(final String property,
      final PropertyChangeListener listener) {
    this.propertyChangeSupport.addPropertyChangeListener(property, listener);
  }

  public List<PortMappingPreset> getPresets() {
    return new ArrayList<>(presets);
  }

  public void setPresets(final List<PortMappingPreset> presets) {
    this.presets = new ArrayList<>(presets);
  }

  public void addPreset(final PortMappingPreset newPreset) {
    final List<PortMappingPreset> oldPresets = new ArrayList<>(this.presets);
    this.presets.add(newPreset);
    this.propertyChangeSupport.firePropertyChange(PROPERTY_PORT_MAPPING_PRESETS, oldPresets,
        new ArrayList<>(this.presets));
  }

  public void removePresets(final PortMappingPreset selectedPreset) {
    final List<PortMappingPreset> oldPresets = new ArrayList<>(this.presets);
    this.presets.remove(selectedPreset);
    this.propertyChangeSupport.firePropertyChange(PROPERTY_PORT_MAPPING_PRESETS, oldPresets,
        new ArrayList<>(this.presets));
  }

  public void savePreset(final PortMappingPreset portMappingPreset) {
    this.propertyChangeSupport.firePropertyChange(PROPERTY_PORT_MAPPING_PRESETS, null,
        new ArrayList<>(this.presets));
  }

  @Override
  public String toString() {
    return "[Settings: presets=" + presets + ", useEntityEncoding=" + useEntityEncoding
        + ", logLevel=" + logLevel
        + ", routerFactoryClassName=" + routerFactoryClassName + "]";
  }

  public boolean isUseEntityEncoding() {
    return useEntityEncoding;
  }

  public void setUseEntityEncoding(final boolean useEntityEncoding) {
    this.useEntityEncoding = useEntityEncoding;
  }

  public String getLogLevel() {
    return this.logLevel;
  }

  public void setLogLevel(final String logLevel) {
    this.logLevel = logLevel;
  }

  public String getRouterFactoryClassName() {
    return routerFactoryClassName;
  }

  public void setRouterFactoryClassName(final String routerFactoryClassName) {
    this.routerFactoryClassName = routerFactoryClassName;
  }
}
