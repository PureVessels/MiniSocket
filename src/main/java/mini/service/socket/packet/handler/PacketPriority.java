package mini.service.socket.packet.handler;

public enum PacketPriority {

    LOW(0),
    NORMAL(1),
    HIGH(2);

    private final int slot;

    PacketPriority(int slot) {
        this.slot = slot;
    }

    public int getSlot() {
        return slot;
    }
}
