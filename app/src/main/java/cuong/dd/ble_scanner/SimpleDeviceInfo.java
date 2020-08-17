package cuong.dd.ble_scanner;

import java.util.Objects;

public class SimpleDeviceInfo {
    String deivceName;
    String deviceMacAddress;
    String rssi;

    public SimpleDeviceInfo(){

    }
    public SimpleDeviceInfo(String deivceName, String deviceMacAddress, String rssi) {

        this.deivceName = deivceName;
        this.deviceMacAddress = deviceMacAddress;
        this.rssi = rssi;
    }


    public String getDeivceName() {
        return deivceName;
    }

    public String getDeviceMacAddress() {
        return deviceMacAddress;
    }

    public String getRssi() {
        return rssi;
    }

    public void setDeivceName(String deivceName) {
        this.deivceName = deivceName;
    }

    public void setDeviceMacAddress(String deviceMacAddress) {
        this.deviceMacAddress = deviceMacAddress;
    }

    public void setRssi(String rssi) {
        this.rssi = rssi;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SimpleDeviceInfo that = (SimpleDeviceInfo) o;

        if(that.getDeviceMacAddress().equals(this.getDeviceMacAddress()))
            return true;
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(deviceMacAddress);
    }
}
