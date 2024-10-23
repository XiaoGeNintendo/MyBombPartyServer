# MyBombPartyServer
This is a basic Kotlin Ktor Websocket server implementation for an imitation of [jklm.fun](https://jklm.fun/)'s minigame: Bomb Party. 
Currently it is used as a practise for myself but you are welcome to do contributions as well!

This project also includes a way-too-simple client for testing. For other clients, please refer to [Unity implementation repo](https://github.com/XiaoGeNintendo/MyBombPartyClientUnity).

# Implementing your own client
OK, in short, there are three websocket entrance points: `/rooms` `/createRoom` and `/join/{roomID}`. All exchanges are in TEXT frames.

After you have made a successful WS connection with the server, before anything else you should send your client version so that the server can check. The current server version can be seen [here](https://github.com/XiaoGeNintendo/MyBombPartyServer/blob/main/src/main/kotlin/top/hhs/xgn/mybombparty/server/MainData.kt#L25) and the tutorial is now on version **"3"** (without quotes).

## (HTTP) /
Visit this page over HTTP to test whether the server has been deployed correctly.

## /rooms
The $2n+1$-th frame will be the ID of the room, and the $2n+2$-th frame will be a serialization of the [GameRoomPreview](https://github.com/XiaoGeNintendo/MyBombPartyServer/blob/main/src/main/kotlin/top/hhs/xgn/mybombparty/data/GameRoomPreview.kt).

**In 4.1 you can visit this page over HTTP as well. This is specially added for Godot**

The server closes the connection after all data are sent. 
## /createRoom
You should send a serialization (JSON) of the [GameRoomPreview](https://github.com/XiaoGeNintendo/MyBombPartyServer/blob/main/src/main/kotlin/top/hhs/xgn/mybombparty/data/GameRoomPreview.kt) you want to create. Player count and state does not matter.

The server closes the connection after receiving.

## /join/{id}
After the client version, you must send the username of the current player. If the room refuses to accommodate you, the connection will be **closed** immediately. If no such room, the connection will be **closed** immediately as well.

The server first sends a serialization of [GameRoom](https://github.com/XiaoGeNintendo/MyBombPartyServer/blob/main/src/main/kotlin/top/hhs/xgn/mybombparty/data/GameRoom.kt). Then several commands will be sent, one in each frame:

### Game State Related
- `startRoom` - the game has started
- `close` - the room is about to close
- `win <username>` - a winner has been found and the game is officially ended

### Ticking
- `countdown <x>` - x/10 seconds left for this turn

### Player
- `new_player <username>` - a new player joins the room
- `disconnect <username>` - someone's net is broken
- `connect <username>` - someone's net is back up again
- `new_spectator <username>` - a new spectator joins
- `kick <username>` - someone is kicked/lost connection during preparation time
### Gameplay
- `type <word>` - the current player types this word. This is not accumulative, which means that if `type shell` is given, the user is just typing 'shell', not add 'shell' to the last `type` command.
- `success <word>` - the confirmed word passes verification
- `fail <word>` - the confirmed word does not pass verification
- `used <word>` - the confirmed word is already used and thus does not pass verification
- `heal <username>` - player adds 1 HP
- `loseLife <username>` - player loses 1 HP
- `new <segment>` - a new topic/word segment is given
- `start <username>` - it's now someone's turn

### User commands
You can send out commands as well (note they are separated with `#`):
- `type#<word>` - player is current typing.
- `confirm#<word>` - player confirms this word
- `kick#<username>` - (only by room master) kick someone
- `start` - (only by master) start the game
- `closeRoom` - (only by master) close the room

The room master is the first person to join the room