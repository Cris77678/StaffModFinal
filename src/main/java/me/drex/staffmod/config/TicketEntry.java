package me.drex.staffmod.config;

import java.util.UUID;

public class TicketEntry {

    public int id;
    public UUID creatorUuid;
    public String creatorName;
    public String message;
    public String status;  // "ABIERTO", "TOMADO", "CERRADO"
    public String handledBy;
    public long createdAt;
    public long updatedAt;

    public TicketEntry(int id, UUID creatorUuid, String creatorName, String message) {
        this.id = id;
        this.creatorUuid = creatorUuid;
        this.creatorName = creatorName;
        this.message = message;
        this.status = "ABIERTO";
        this.handledBy = "";
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }
}
