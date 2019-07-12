/**
 *
 */
package cn.tzauto.octopus.secsLayer.domain;

import cn.tzauto.octopus.secsLayer.exception.BehaviorStateException;
import cn.tzauto.octopus.secsLayer.util.NormalConstant;


public class EquipState  {
    //E98 service states

    public static final int IN_SERVICE_STATE = 1;
    public static final int OUT_OF_SERVICE_STATE = 0; //default
    //E98 behavior states
    public static final int IDLE_STATE = 8;
    public static final int INACTIVE = 32;
    public static final int INACTIVE_IDLE_WITH_ALARMS_STATE = 33;
    public static final int INACTIVE_STOPPED_STATE = 34;
    public static final int INACTIVE_ABORTED_STATE = 35;
    public static final int ACTIVE = 64;
    public static final int ACTIVE_ACTIVE_SERVICE_STATE = 65;
    public static final int ACTIVE_STOPPING_STATE = 66;
    public static final int ACTIVE_ABORTING_STATE = 67;
    public static final int ACTIVE_PAUSE = 68;
    public static final int ACTIVE_PAUSE_PAUSING_STATE = 69;
    public static final int ACTIVE_PAUSE_PAUSED_STATE = 70;
    private int behaviorState;
    private int serviceState;
    private boolean commOn;
    private String eventString;
    private boolean isAlarm;
    private String runState;
    private String runningRcp;
    private String workLot;
    private int alarmState;
    private String controlState;

    private boolean netConnect;

    public EquipState() {
        behaviorState = IDLE_STATE;
        serviceState = OUT_OF_SERVICE_STATE;
        commOn = false;
        isAlarm = false;
        eventString = NormalConstant.NO_EVENT_REPORT;
        runState = "--";
        workLot = "--";
        runningRcp = "--";
        alarmState = 0;
        netConnect = true;
    }

    public EquipState(int behavior_state, int service_state) {
        behaviorState = behavior_state;
        serviceState = service_state;
        runState = "--";
        workLot = "--";
        runningRcp = "--";
        alarmState = 0;
        //eventString = "No Event Reported";
    }

    public void transitBehaviorState(int newBehaviorState)
            throws IllegalArgumentException, BehaviorStateException {
        if (!isLegalBehaviorState(newBehaviorState)) {
            throw new IllegalArgumentException("Argument = " + newBehaviorState + ". It is a wrong value!");
        }
        switch (newBehaviorState) {
            case IDLE_STATE:
                if (this.behaviorState != ACTIVE_ACTIVE_SERVICE_STATE
                        && this.behaviorState != INACTIVE_IDLE_WITH_ALARMS_STATE
                        && this.behaviorState != INACTIVE_STOPPED_STATE
                        && this.behaviorState != INACTIVE_ABORTED_STATE) {
                    throw new BehaviorStateException("Wrong Behavior State transition: current state: "
                            + this.mapBehaviorStateToString(this.behaviorState) + " target state: IDLE_STATE.");
                }
                break;
            case INACTIVE_IDLE_WITH_ALARMS_STATE:
                break;
            case INACTIVE_STOPPED_STATE:
                if (this.behaviorState != ACTIVE_STOPPING_STATE) {
                    throw new BehaviorStateException("Wrong Behavior State transition: current state: "
                            + this.mapBehaviorStateToString(this.behaviorState) + " target state: INACTIVE_STOPPED_STATE.");
                }
                break;
            case INACTIVE_ABORTED_STATE:
                if (this.behaviorState != ACTIVE_ABORTING_STATE) {
                    throw new BehaviorStateException("Wrong Behavior State transition: current state: "
                            + this.mapBehaviorStateToString(this.behaviorState) + " target state: INACTIVE_ABORTED_STATE.");
                }
                break;
            case ACTIVE_ACTIVE_SERVICE_STATE:
                if (this.behaviorState != IDLE_STATE
                        && this.behaviorState != ACTIVE_PAUSE_PAUSED_STATE) {
                    throw new BehaviorStateException("Wrong Behavior State transition: current state: "
                            + this.mapBehaviorStateToString(this.behaviorState) + " target state: ACTIVE_ACTIVE_SERVICE_STATE.");
                }
                break;
            case ACTIVE_STOPPING_STATE:
                if (this.behaviorState != ACTIVE_ACTIVE_SERVICE_STATE
                        && this.behaviorState != ACTIVE_PAUSE_PAUSED_STATE) {
                    throw new BehaviorStateException("Wrong Behavior State transition: current state: "
                            + this.mapBehaviorStateToString(this.behaviorState) + " target state: ACTIVE_STOPPING_STATE.");
                }
                break;
            case ACTIVE_ABORTING_STATE:
                if (this.behaviorState != ACTIVE_ACTIVE_SERVICE_STATE
                        && this.behaviorState != ACTIVE_PAUSE_PAUSED_STATE) {
                    throw new BehaviorStateException("Wrong Behavior State transition: current state: "
                            + this.mapBehaviorStateToString(this.behaviorState) + " target state: ACTIVE_ABORTING_STATE.");
                }
                break;
            case ACTIVE_PAUSE_PAUSING_STATE:
                if (this.behaviorState != ACTIVE_ACTIVE_SERVICE_STATE) {
                    throw new BehaviorStateException("Wrong Behavior State transition: current state: "
                            + this.mapBehaviorStateToString(this.behaviorState) + " target state: ACTIVE_PAUSE_PAUSING_STATE.");
                }
                break;
            case ACTIVE_PAUSE_PAUSED_STATE:
                if (this.behaviorState != ACTIVE_PAUSE_PAUSING_STATE) {
                    throw new BehaviorStateException("Wrong Behavior State transition: current state: "
                            + this.mapBehaviorStateToString(this.behaviorState) + " target state: ACTIVE_PAUSE_PAUSED_STATE.");
                }
                break;
        }
        this.behaviorState = newBehaviorState;
    }

    public void setInitialBehaviorState(int newBehaviorState)
            throws IllegalArgumentException {
        if (!isLegalBehaviorState(newBehaviorState)) {
            throw new IllegalArgumentException("setInitialBehaviorState-Argument = "
                    + newBehaviorState + ". It is a wrong value!");
        }
        this.behaviorState = newBehaviorState;
    }

    public boolean isLegalBehaviorState(int newBehaviorState) {
        return newBehaviorState == INACTIVE_IDLE_WITH_ALARMS_STATE
                || newBehaviorState == INACTIVE_STOPPED_STATE
                || newBehaviorState == INACTIVE_ABORTED_STATE
                || newBehaviorState == ACTIVE_ACTIVE_SERVICE_STATE
                || newBehaviorState == ACTIVE_STOPPING_STATE
                || newBehaviorState == ACTIVE_ABORTING_STATE
                || newBehaviorState == ACTIVE_PAUSE_PAUSING_STATE
                || newBehaviorState == ACTIVE_PAUSE_PAUSED_STATE
                || newBehaviorState == IDLE_STATE;
    }

    public void transitServiceState(int newServiceState)
            throws IllegalArgumentException {
        if (newServiceState != IN_SERVICE_STATE && newServiceState != OUT_OF_SERVICE_STATE) {
            throw new IllegalArgumentException("Argument = " + newServiceState + ". It should be "
                    + IN_SERVICE_STATE + " or " + OUT_OF_SERVICE_STATE);
        }
        this.serviceState = newServiceState;
    }

    public int getBehaviorState() {
        return behaviorState;
    }

    public String getBehaviorStateAsString() {
        return this.mapBehaviorStateToString(this.behaviorState);
    }

    public int getServiceState() {
        return serviceState;
    }

    public String mapBehaviorStateToString(int newBehaviorState)
            throws IllegalArgumentException {
        if (!isLegalBehaviorState(newBehaviorState)) {
            throw new IllegalArgumentException("mapBehaviorStateToString Argument = " + newBehaviorState
                    + ". It is a wrong value!");
        }
        switch (newBehaviorState) {
            case IDLE_STATE:
                return "IDLE_STATE";
            case INACTIVE_IDLE_WITH_ALARMS_STATE:
                return "INACTIVE_IDLE_WITH_ALARMS_STATE";
            case INACTIVE_STOPPED_STATE:
                return "INACTIVE_STOPPED_STATE";
            case INACTIVE_ABORTED_STATE:
                return "INACTIVE_ABORTED_STATE";
            case ACTIVE_ACTIVE_SERVICE_STATE:
                return "ACTIVE_SERVICE_STATE";
            case ACTIVE_STOPPING_STATE:
                return "ACTIVE_STOPPING_STATE";
            case ACTIVE_ABORTING_STATE:
                return "ACTIVE_ABORTING_STATE";
            case ACTIVE_PAUSE_PAUSING_STATE:
                return "ACTIVE_PAUSE_PAUSING_STATE";
            case ACTIVE_PAUSE_PAUSED_STATE:
                return "ACTIVE_PAUSE_PAUSED_STATE";
        }
        return "";
    }

    public String toString() {
        return "service = "
                + (this.serviceState == IN_SERVICE_STATE ? "IN_SERVICE_STATE" : "OUT_OF_SERVICE_STATE")
                + "; Behavior State = " + this.mapBehaviorStateToString(this.behaviorState);
    }

    public boolean isCommOn() {
        return commOn;
    }

    public void setCommOn(boolean commOn) {
        this.commOn = commOn;
    }

    public String getEventString() {
        return eventString;
    }

    public void setEventString(String eventString) {
        this.eventString = eventString;
    }

    @Override
    public Object clone() {
        EquipState another = new EquipState(this.behaviorState, this.serviceState);
        another.setCommOn(this.isCommOn());
        another.setAlarmState(alarmState);
        another.setAlarm(isAlarm);
        another.setEventString(this.getEventString());
        another.setNetConnect(this.isNetConnect());
        return another;
    }

    /**
     * @return the alarmState
     */
    public boolean isAlarm() {
        return isAlarm;
    }

    public void setAlarm(boolean isAlarm) {
        this.isAlarm = isAlarm;
    }

    /**
     * @param alarmState the alarmState to set
     */
    public void setAlarmState(int alarmState) {
        if (alarmState == 0) {
            setAlarm(false);
        } else {
            setAlarm(true);
        }
        this.alarmState = alarmState;
    }

    public int getAlarmState() {
        return alarmState;
    }

    public String getRunState() {
        return runState;
    }

    public void setRunState(String runState) {
        this.runState = runState;
    }

    public String getRunningRcp() {
        return runningRcp;
    }

    public void setRunningRcp(String runningRcp) {
        this.runningRcp = runningRcp;
    }

    public String getWorkLot() {
        return workLot;
    }

    public void setWorkLot(String workLot) {
        this.workLot = workLot;
    }

    public String getControlState() {
        return controlState;
    }

    public void setControlState(String controlState) {
        this.controlState = controlState;
    }

    public boolean isNetConnect() {
        return netConnect;
    }

    public void setNetConnect(boolean netConnect) {
        this.netConnect = netConnect;
    }
}
