package edu.illinois.mitra.template;

public class BotInfo {
    public final String name;
    public final String typeName;
    public final String mac;
    public final String device;
    public final String ip;

    BotInfo(String name, String typeName, String mac, String device, String ip) {
        this.name = name;
        this.typeName = typeName;
        this.mac = mac;
        this.device = device;
        this.ip = ip;
    }
}