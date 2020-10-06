package com.rax.autostabilizer.Models;

public class Stabilizer {
    String name;
    String IPAddress;
    int port;
    String macAddress;

    public Stabilizer(String name, String IPAddress, int port, String macAddress) {
        this.name = name;
        this.IPAddress = IPAddress;
        this.port = port;
        this.macAddress = macAddress;
    }

    public String getName() {
        return name;
    }

    public String getIPAddress() {
        return IPAddress;
    }

    public int getPort() {
        return port;
    }

    public String getMacAddress() {
        return macAddress;
    }
}
