package ru.mevgeniy.malinaticket.model;

public enum TicketStatus {
    OPEN("Открыт"),
    CLOSED("Закрыт"),
    DELETED("Удален");

    private final String displayName;

    TicketStatus(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public boolean canReceiveComments() {
        return this == OPEN;
    }

    public boolean canBeClosed() {
        return this == OPEN;
    }

    public boolean canBeReopened() {
        return this == CLOSED;
    }

    public boolean canBeAssigned() {
        return this == OPEN;
    }

    public boolean canBeSoftDeleted() {
        return this != DELETED;
    }

    public boolean canBePurged() {
        return this == DELETED;
    }

    public static TicketStatus fromString(String value) {
        if (value == null) {
            return OPEN;
        }
        for (TicketStatus status : values()) {
            if (status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }
        return OPEN;
    }
}
