# MiniSocket

MiniSocket is a lightweight Java socket communication library designed for persistent backend-client communication.

It provides a simple abstraction over Java TCP sockets with built-in connection management, automatic reconnection, packet handling, authentication, and threaded communication.

## Features

* TCP backend-client communication
* Persistent socket connections
* Automatic client reconnection
* Connection lifecycle management
* Token-based authentication
* JSON-based packet protocol
* Packet handler system
* Thread-safe packet sending
* Custom client connection callbacks
* Lightweight and dependency-friendly API

## Architecture

MiniSocket separates socket management, connection lifecycle, and packet processing into dedicated components.

### Backend

```text
SocketBackend
│
├── ClientSocketThread
│       └── PacketHandler
│
└── Client Connections
```

### Client

```text
SocketClient
│
└── ConnectionBackendThread
        │
        └── ClientConnectionThread
                └── PacketHandler
```

## Backend

`SocketBackend` listens for incoming TCP connections and creates a dedicated connection thread for each connected client.

```java
new MiniSocket(
        backendData,
        "your-token",
        databaseService    // Optional
).start("your-socket-port");
```

## Client

`SocketClient` manages the connection to a socket backend and automatically attempts to reconnect when the connection is lost.

```java
SocketClient client = new SocketClient(
        "your-client-name", 
        "your-socket-ip",
        "your-socket-port",
        "your-token",
        socketClient -> {
            // Called after a successful connection.
        } // Optional
);
```

The client maintains the connection automatically:

```text
Connect
   ↓
Connected
   ↓
Connection lost
   ↓
Wait
   ↓
Reconnect
   ↓
Connected
```

## Packets

Communication is based on JSON packets.

A packet contains a channel and a packet body.

Example:

```json
{
    "token": "your-token",
    "client": "example-client",
    "packet": {
        "post": "example_channel",
        "body": {
            "message": "Hello!"
        }
    }
}
```

Packets are represented by `PacketData` and can be sent using the `SocketClient`.

### Sending a Packet from Client to Backend

```java
import com.google.gson.JsonObject;
import mini.service.socket.client.SocketClient;
import mini.service.socket.packet.Packet;
import mini.service.socket.packet.PacketData;

SocketClient client = ...;

Packet packet = ...;

client.send(packet);
// or
client.send(packet.build());

JsonObject body = ...;

PacketData packetData = new PacketData(
        "target-channel",
        body
);

client.send(packetData);
```

### Sending a Packet from Backend to Client

```java
import java.net.Socket;

import mini.service.socket.backend.ClientSocketThread;
import mini.service.socket.backend.MainBackend;
import mini.service.socket.packet.Packet;
import mini.service.socket.packet.PacketData;

MainBackend backend = ...;
Socket target = ...;

Packet packet = ...;
PacketData packetData = new PacketData(
        "target-channel",
        body
);

backend.send(packet, target);
// or
backend.send(packet.build(), target);

backend.send(packetData, target);
```

A packet can also be sent directly through the connection thread:

```java
ClientSocketThread socketThread = ...;

socketThread.send(packet);
// or
socketThread.send(packet.build());

socketThread.send(packetData);
```

## Packet Handlers

Incoming packets are routed through registered `PacketHandler` instances.

A handler can define where a packet is expected to be processed and whether additional connection information is required.

Conceptually:

```text
Incoming JSON
     ↓
 PacketData
     ↓
   Channel
     ↓
PacketHandler
     ↓
Handler execution
```

This allows applications to keep transport logic separate from application logic.

## Authentication

Connections support token-based authentication.

The client provides a token when sending packets, while the backend validates the received token before processing the packet.

Default or empty tokens should only be used during development.

## Threading

MiniSocket uses dedicated threads for persistent connections.

A client connection consists of:

```text
ConnectionBackendThread
│
├── Connection / Reconnection Lifecycle
│
└── ClientConnectionThread
        └── Incoming Packet Processing
```

The backend creates a dedicated connection thread for each connected client.

Packet sending is synchronized per connection to prevent concurrent writes from corrupting the packet stream.

## Requirements

* Java 21+
* TCP networking support
* [MiniBase](https://github.com/PureVessels/MiniBase) (If you are going to use a database)

## Design Goals

MiniSocket is intentionally designed to remain independent of any specific application or platform.

It can be used for:

* Java applications
* Backend services
* Game servers
* Proxies
* Internal service communication
* Custom backend-client applications

The library does not depend on Minecraft or any specific client implementation.
