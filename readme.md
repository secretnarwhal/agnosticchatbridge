# ACB: (Version) Agnostic Chat Bridge
Motivation: *I threw this awful code together because of how often Minecraft updates and because I thought it'd be cool. It isn't.*

This is a chat bridge that runs the server as a subprocess (see note at bottom, not child process) and relies solely on standard input and standard output of Minecraft to bridge messages from and to Discord.

## Compilation
`gradlew shadowJar` \
Alternatively, download the last artifact from actions.

## Usage
Not recommended. \
If you still want to use this, you have to run the server *through* the chat bridge now. \
`java -jar chatbridge-*.jar [CONFIG FILE]` where the config file may optionally be supplied.

## Configuration
You must supply a `token` and a `channel`. \
Optionally, you may supply a `webhook` directly, which will be created if not provided.

Lastly, you must provide the server starting commandline in `cmdline`.

### Patterns

The RegEx patterns and message template will be populated adjusted for the latest Minecraft version (1.21.8 at time of writing), but can be changed later.

1. `mePattern` determines when a player has sent a /me message.
2. `messagePattern` determines when a player has sent a standard message.
3. `startPattern` determines when the server has "started" (ready for players).
4. `joinPattern` determines when a player has joined the server.
5. `leavePattern` determines when a player has left the server.
6. `deathPattern` determines when a player has died on the server.
7. `advancementPattern` determines when a player has achieved an advancement.

A preceding pattern, if it applies, cancels processing of all further patterns.
Meaning if a message passes the `mePattern` check, it is not eligible for the `deathPattern` check, to prevent abuse.

### Templates

Additionally, for Discord to Minecraft messaging, you may configure `receiveTemplate` with a command to run when a discord message is received in the configured channel. You may use the following variables:
- `%name%` Sender Discord Username
- `%id` Sender Discord ID
- `%message%` Message without formatting


# WARNING: Close your servers properly!
For convenience sake, `Runtime.exec` is used to start the actual server. Which means that if you close the chat bridge first, the server will not shut down.
