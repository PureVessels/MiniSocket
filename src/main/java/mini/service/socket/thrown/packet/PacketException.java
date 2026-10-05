package mini.service.socket.thrown.packet;

public class PacketException extends RuntimeException {

    public PacketException(String message, Function function) {
        super("Packet " + function + " function failed: " + message);
    }

    public enum Function {
        CREATE,
        LOAD,
        READER,
        CONTROL
    }
}
