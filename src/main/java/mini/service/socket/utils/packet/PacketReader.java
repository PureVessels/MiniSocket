package mini.service.socket.utils.packet;

public enum PacketReader {

    BACKEND,
    CLIENT

    ;

    public boolean isBackendReader(){ return this == BACKEND; }
    public boolean isClientReader(){ return this == CLIENT; }
}
