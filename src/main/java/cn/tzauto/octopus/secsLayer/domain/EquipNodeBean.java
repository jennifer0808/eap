package cn.tzauto.octopus.secsLayer.domain;

import java.beans.*;
import java.io.Serializable;
import org.apache.log4j.Logger;

public class EquipNodeBean implements Serializable {

    public static final String EQUIP_STATE_PROPERTY = "EquipStateProperty";
    public static final String EQUIP_PANEL_PROPERTY = "EquipPanelProperty";
    private PropertyChangeSupport propertySupport;
    private String equipName;
    //private String equipIdProperty;
    private String deviceIdProperty;
    private boolean startUpProperty;
    private String smlFilePathProperty;
    private String localIPAddressProperty;
    private int localTCPPortProperty;
    private String remoteIPAddressProperty;
    private int remoteTCPPortProperty;
    private String connectModeProperty; //only "active" or "passive" allowed, default is "active"
    private String protocolTypeProperty;//only "hsms" or "rs232" allowed, default is "hsms"
    private int alarmProperty;//0 no alarm  1 little alarm  2 alarm and hold
    private EquipState equipStateProperty; //if equipment state is changed, this instance must be replaced.
    private static final Logger logger = Logger.getLogger(EquipNodeBean.class.getName());
    private String iconPath;
    private EquipPanel equipPanelProperty;

    public EquipNodeBean() {
        propertySupport = new PropertyChangeSupport(this);
        equipStateProperty = new EquipState();
    }

    public EquipNodeBean(EquipState equipStateProperty) {
        propertySupport = new PropertyChangeSupport(this);
        this.equipStateProperty = equipStateProperty;
        this.equipPanelProperty = new EquipPanel();
    }

    public boolean isOutOfService() {
        return equipStateProperty.getServiceState() == EquipState.OUT_OF_SERVICE_STATE;
    }

    public EquipState getEquipStateProperty() {
        return equipStateProperty;
    }

    public void setEquipStateProperty(EquipState value) {
        EquipState oldValue = equipStateProperty;
        equipStateProperty = value;
        logger.debug(EQUIP_STATE_PROPERTY + "Property has been changed, old value = " + oldValue + " new value =" + value);
        propertySupport.firePropertyChange(EQUIP_STATE_PROPERTY, oldValue, equipStateProperty);
    }

    public EquipPanel getEquipPanelProperty() {
        return equipPanelProperty;
    }

    public void setEquipPanelProperty(EquipPanel value) {
        EquipPanel oldValue = equipPanelProperty;
        equipPanelProperty = value;
        logger.debug(EQUIP_PANEL_PROPERTY + "Property has been changed, old value = " + oldValue + " new value =" + value);
        propertySupport.firePropertyChange(EQUIP_PANEL_PROPERTY, oldValue, equipPanelProperty);
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertySupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener) {
        propertySupport.removePropertyChangeListener(listener);
    }

    /**
     * @return the deviceIdProperty
     */
    public String getDeviceIdProperty() {
        return deviceIdProperty;
    }

    /**
     * @param deviceIdProperty the deviceIdProperty to set
     */
    public void setDeviceIdProperty(String deviceIdProperty) {
        this.deviceIdProperty = deviceIdProperty;
    }

    /**
     * @return the startUpProperty
     */
    public boolean isStartUpProperty() {
        return startUpProperty;
    }

    /**
     * @param startUpProperty the startUpProperty to set
     */
    public void setStartUpProperty(boolean startUpProperty) {
        this.startUpProperty = startUpProperty;
    }

    /**
     * @return the smlFilePathProperty
     */
    public String getSmlFilePathProperty() {
        return smlFilePathProperty;
    }

    /**
     * @param smlFilePathProperty the smlFilePathProperty to set
     */
    public void setSmlFilePathProperty(String smlFilePathProperty) {
        this.smlFilePathProperty = smlFilePathProperty;
    }

    /**
     * @return the localIPAddressProperty
     */
    public String getLocalIPAddressProperty() {
        return localIPAddressProperty;
    }

    /**
     * @param localIPAddressProperty the localIPAddressProperty to set
     */
    public void setLocalIPAddressProperty(String localIPAddressProperty) {
        this.localIPAddressProperty = localIPAddressProperty;
    }

    /**
     * @return the localTCPPortProperty
     */
    public int getLocalTCPPortProperty() {
        return localTCPPortProperty;
    }

    /**
     * @param localTCPPortProperty the localTCPPortProperty to set
     */
    public void setLocalTCPPortProperty(int localTCPPortProperty) {
        this.localTCPPortProperty = localTCPPortProperty;
    }

    /**
     * @return the remoteIPAddressProperty
     */
    public String getRemoteIPAddressProperty() {
        return remoteIPAddressProperty;
    }

    /**
     * @param remoteIPAddressProperty the remoteIPAddressProperty to set
     */
    public void setRemoteIPAddressProperty(String remoteIPAddressProperty) {
        this.remoteIPAddressProperty = remoteIPAddressProperty;
    }

    /**
     * @return the remoteTCPPortProperty
     */
    public int getRemoteTCPPortProperty() {
        return remoteTCPPortProperty;
    }

    /**
     * @param remoteTCPPortProperty the remoteTCPPortProperty to set
     */
    public void setRemoteTCPPortProperty(int remoteTCPPortProperty) {
        this.remoteTCPPortProperty = remoteTCPPortProperty;
    }

    /**
     * @return the connectModeProperty
     */
    public String getConnectModeProperty() {
        return connectModeProperty;
    }

    /**
     * @param connectModeProperty the connectModeProperty to set
     */
    public void setConnectModeProperty(String connectModeProperty) {
        this.connectModeProperty = connectModeProperty;
    }

    /**
     * @return the protocolTypeProperty
     */
    public String getProtocolTypeProperty() {
        return protocolTypeProperty;
    }

    /**
     * @param protocolTypeProperty the protocolTypeProperty to set
     */
    public void setProtocolTypeProperty(String protocolTypeProperty) {
        this.protocolTypeProperty = protocolTypeProperty;
    }

    /**
     * @return the equipName
     */
    public String getEquipName() {
        return equipName;
    }

    /**
     * @param equipName the equipName to set
     */
    public void setEquipName(String equipName) {
        this.equipName = equipName;
    }

    @Override
    public String toString() {
        return this.equipName + " dev id " + this.deviceIdProperty;
    }

    /**
     * @return the alarmProperty
     */
    public int getAlarmProperty() {
        return equipStateProperty.getAlarmState();//alarmProperty;
    }

    /**
     * @param alarmProperty the alarmProperty to set
     */
    public void setAlarmProperty(int alarmProperty) {
        this.alarmProperty = alarmProperty;
        equipStateProperty.setAlarmState(alarmProperty);
    }

    public String getIconPath() {
        return iconPath;
    }

    public void setIconPath(String iconPath) {
        this.iconPath = iconPath;
    }
}
