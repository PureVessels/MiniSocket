package mini.service.socket.packet.handler;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface PacketHandler {

    String post();
    String[] ignoreClient() default {};

    PacketPriority priority() default PacketPriority.NORMAL;
}
