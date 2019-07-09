package cn.tzauto.octopus.biz.tooling;

/**
 * Created by tzauto on 2019/7/4.
 */
public class LaserCrystal {
    private String ap_no;
    private String axis;
    private String crystalNo;
    private String power;
    private String accuracy;
    private boolean isCrystalAccuracy;

    public boolean isCrystalAccuracy() {
        return this.isCrystalAccuracy;
    }

    public boolean canPowerCheck() {
        if (this.getPower() != null && this.getAp_no() != null && this.getAxis() != null) {
            return true;
        } else {

            return false;
        }
    }

    public boolean canAccuracyCheck() {
        if (this.getAccuracy() != null && this.getAp_no() != null && this.getAxis() != null) {
            return true;
        } else {

            return false;
        }
    }

    @Override
    public String toString() {
        String str = this.getAxis() + "_" + this.getAp_no() + "_POWER=" + this.getPower();
        return str;
    }

    public String toAccuracyString() {
        String str = this.getAxis() + "_" + this.getAp_no() + "_ACCURACY=" + this.getAccuracy();
        return str;
    }

    public String getAp_no() {
        return ap_no;
    }

    public void setAp_no(String ap_no) {
        this.ap_no = ap_no;
    }

    public String getAxis() {
        return axis;
    }

    public void setAxis(String axis) {
        this.axis = axis;
    }

    public String getCrystalNo() {
        return crystalNo;
    }

    public void setCrystalNo(String crystalNo) {
        this.crystalNo = crystalNo;
    }

    public String getPower() {
        double powertemp = Double.parseDouble(this.power) / 1000;
        return String.valueOf(powertemp);
    }

    public void setPower(String power) {
        this.power = power;
    }

    public String getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(String accuracy) {
        this.accuracy = accuracy;
    }

    public void setCrystalAccuracy(boolean crystalAccuracy) {
        isCrystalAccuracy = crystalAccuracy;
    }
}
