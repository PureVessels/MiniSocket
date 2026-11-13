package mini.service.socket;

import mini.service.socket.packet.handler.PacketHandler;
import mini.service.socket.packet.handler.PacketListener;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class RegisterHandlers {

    private static final Comparator<RegisteredListener> PRIORITY_ORDER = Comparator.comparingInt((RegisteredListener r) -> r.priority().getSlot()).reversed();

    private final Map<String, List<RegisteredListener>> handlers = new ConcurrentHashMap<>();
    private final MiniSocket instance;

    RegisterHandlers(MiniSocket socket) {
        this.instance = socket;
    }

    public void register(@NotNull PacketListener listener, @NotNull String token) {
        PacketHandler handler = getAnnotation(listener);
        checkToken(handler.post(), token);

        RegisteredListener registered = new RegisteredListener(handler, listener);

        handlers.compute(handler.post(), (post, list) -> {
            if (list != null && list.contains(registered)) return list;

            List<RegisteredListener> sorted = list == null ? new ArrayList<>() : new ArrayList<>(list);
            sorted.add(registered);
            sorted.sort(PRIORITY_ORDER);

            return List.copyOf(sorted);
        });
    }

    public void unregister(@NotNull PacketListener listener, @NotNull String token) {
        PacketHandler handler = getAnnotation(listener);
        checkToken(handler.post(), token);

        handlers.computeIfPresent(handler.post(), (post, list) -> {
            List<RegisteredListener> remaining = list.stream().filter(r -> r.listener() != listener).toList();
            return remaining.isEmpty() ? null : remaining;
        });
    }

    public List<RegisteredListener> getHandlers(String post) { return handlers.getOrDefault(post, List.of()); }
    public int getHandlerCount(String post) { return getHandlers(post).size(); }

    private PacketHandler getAnnotation(PacketListener listener) {
        PacketHandler handler = listener.getClass().getAnnotation(PacketHandler.class);
        if (handler == null) throw new IllegalArgumentException(listener.getClass().getName() + " has no @PacketHandler annotation");

        return handler;
    }

    private void checkToken(String post, String token) {
        if (instance.tokenValid(token)) return;

        MiniSocket.getLogger().warning("Packet listener rejected (invalid token): " + post);
        throw new SecurityException("Invalid token for packet listener: " + post);
    }
}
