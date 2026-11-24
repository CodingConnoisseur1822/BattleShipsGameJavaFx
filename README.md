# Multiplayer JavaFX Battleship

A Battleship desktop game written in Java and JavaFX. Play against the computer, or log in and play online against another person using a room code. Wins and losses are saved in a PostgreSQL database and shown on a leaderboard.

## Features

- **Single-player mode** against a computer opponent that places its ships randomly and never fires at the same square twice
- **Online two-player mode** using a room code: create a room or pick an open one from the lobby list
- **Accounts**: register and log in, with passwords stored as salted bcrypt hashes
- **Leaderboard** showing the top 50 players by wins
- **Automatic win/loss tracking** when a match ends, including when a player quits or disconnects (the other player wins by forfeit)
- **Drag-and-drop ship placement** with rotation and checks for ships going off the board or overlapping
- Ships are revealed on the enemy board when they are sunk

## How to play

1. Drag your ships onto your grid (left board).
2. Rotate a ship while holding it with **right-click** or the **R** key.
3. Click **Start Game**. In online mode, the game begins once both players are ready.
4. Click a square on the enemy grid (right board) to fire.
5. Sink all of the enemy's ships to win.

## Tech stack

- Java
- JavaFX (UI and canvas drawing)
- TCP sockets with a multi-threaded server
- PostgreSQL with JDBC (`PreparedStatement`s and the `pgcrypto` extension for bcrypt)

## Project structure

```
application/   Main entry point, game loop and control panel
controller/    InputManager (mouse and keyboard controls)
database/      UserDao, DatabaseConnection, AuthResult
model/         GameState, Board, Ship, Fleet, Enemy, AccountRules, ...
network/       GameServer, GameRoom, ServerClientHandler, GameClient, NetworkCommand
ui/            TitleScreen, LoginScreen, LobbyScreen, ScoreboardScreen
util/          SceneFactory, ServerRequest, UserSession, Load
```

## How it works

- **Server** (`GameServer`) listens for connections and starts a new thread for each client. Rooms are matched by room code in `GameRoom`.
- **Requests** like login, register, listing rooms and the scoreboard are short one-off messages. Joining a room keeps the connection open so moves can be passed to the opponent.
- **Messages** are plain text lines such as `ATTACK:3,5` or `ATTACK_RESULT:3,5:true`, defined in `NetworkCommand`.
- **Database**: `UserDao` registers users, checks logins, updates wins and losses in a single transaction (both updates succeed or neither does), and loads the scoreboard.

## Setup

### Requirements

- Java (version used: [add your version])
- JavaFX SDK (version used: [add your version])
- PostgreSQL
- PostgreSQL JDBC driver

### 1. Create the database table

Run this in your PostgreSQL database:

```sql
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(20) NOT NULL,
    password_hash TEXT NOT NULL,
    wins INT NOT NULL DEFAULT 0,
    losses INT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX users_username_lower_idx ON users (LOWER(username));
```

### 2. Set your database details

Open `database/DatabaseConnection.java` and change the URL, user and password to match your own PostgreSQL setup. Do not commit your real password to GitHub.

### 3. Start the server

Run `network.GameServer`. It listens on port `5555` (set in `network/ServerConfig.java`).

### 4. Start the game

Run `application.Main`. To play online from another computer, change `HOST` in `ServerConfig.java` to the server's address.

## Possible improvements

- Read database details from environment variables instead of the source code
- Encrypt the connection between client and server (it currently uses plain text sockets, so it is only suitable for local or learning use)
- Add a smarter computer opponent that targets squares next to a hit

## Author

Eduard-Alexandru Idu - [github.com/codingconnoseiur1822](https://github.com/codingconnoseiur1822)
