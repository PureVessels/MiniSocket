package mini.service.socket.thrown.packet;

public class PacketCreateException extends RuntimeException {

    public PacketCreateException(String message, Function function) {
        super("Packet " + function + " function failed " + message);
    }

    public enum Function {
        CREATE,
        LOAD,

    }
}
