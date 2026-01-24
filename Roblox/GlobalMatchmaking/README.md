# GlobalMatchmaking

A simple Roblox matchmaking wrapper for [Namazu Elements](https://namazustudios.com/).

## Installation

1. Copy the `GlobalMatchmaking` folder to `ServerStorage` in your Roblox game
2. Enable `HttpService` in Game Settings → Security
3. Set up your Namazu Elements instance and create a matchmaking configuration in the admin panel

## Quick Start

```lua
--[[
	Example: Auto-Queue Lobby
	Place in ServerScriptService
]]

local Players = game:GetService("Players")
local GlobalMatchmaking = require(game.ServerStorage.GlobalMatchmaking)

----- CONFIG -----
local URL = "https://your-instance.example.com"  -- Your Namazu Elements URL
local SECRET = "your-robloxkit-secret"           -- Your RobloxKit secret
local APP = "your_app"                           -- Your application name
local CONFIG = "default"                         -- Matchmaking config name (create in admin panel)
local PLACE_ID = 123456789                       -- Your game place ID
local MIN_PLAYERS = 2                            -- Players needed to start
------------------

GlobalMatchmaking:Login(URL, SECRET, APP, {
	DefaultPlaceId = PLACE_ID,
	DefaultMinPlayers = MIN_PLAYERS,
})

Players.PlayerAdded:Connect(function(player)
	task.wait(1)
	
	GlobalMatchmaking:Queue(CONFIG, player, {
		JoinedQueue = function()
			print(player.Name .. " queued")
		end,
		MatchFound = function()
			print(player.Name .. " matched!")
			GlobalMatchmaking:Teleport(player)
		end,
	})
end)
```

## API Reference

### `GlobalMatchmaking:Login(baseUrl, secret, appName, options?)`

Initialize the matchmaking service. Call once at startup.

| Parameter | Type | Description |
|-----------|------|-------------|
| `baseUrl` | string | Your Namazu Elements instance URL |
| `secret` | string | Your RobloxKit secret key |
| `appName` | string | Your application name |
| `options` | table? | Optional defaults (see below) |

**Options:**
```lua
{
    DefaultConfigName = "default",  -- Matchmaking config name
    DefaultPlaceId = 123456789,     -- Game place ID for teleporting
    DefaultMinPlayers = 2,          -- Min players to start match
}
```

---

### `GlobalMatchmaking:Queue(options, player, callbacks?)`

Queue a player for matchmaking.

| Parameter | Type | Description |
|-----------|------|-------------|
| `options` | string \| table | Config name or options table |
| `player` | Player | The player to queue |
| `callbacks` | table? | Event callbacks |

**Options table:**
```lua
{
    ConfigName = "ranked",   -- Matchmaking config name
    PlaceId = 123456789,     -- Override place ID
    MinPlayers = 4,          -- Override min players
}
```

**Callbacks:**
```lua
{
    JoinedQueue = function()
        -- Player entered the queue
    end,
    MatchFound = function(match)
        -- Match is ready, teleport the player
    end,
    QueueFailed = function(error)
        -- Something went wrong
    end,
}
```

---

### `GlobalMatchmaking:Cancel(player)`

Remove a player from their current queue.

---

### `GlobalMatchmaking:InQueue(player) → boolean`

Check if a player is currently queuing.

---

### `GlobalMatchmaking:GetMatch(player) → MatchResponse?`

Get the current match data for a player.

---

### `GlobalMatchmaking:Teleport(player)`

Teleport a player to their match's reserved server.

---

## Examples

### Basic Lobby (Auto-Queue)

```lua
local Players = game:GetService("Players")
local GlobalMatchmaking = require(game.ServerStorage.GlobalMatchmaking)

GlobalMatchmaking:Login(
    "https://production.elements.example.com",
    "your-secret",
    "my_game"
)

Players.PlayerAdded:Connect(function(player)
    task.wait(1)
    
    if GlobalMatchmaking:InQueue(player) then return end
    
    GlobalMatchmaking:Queue("default", player, {
        JoinedQueue = function()
            print(player.Name .. " joined queue")
        end,
        MatchFound = function(match)
            print(player.Name .. " found match!")
            GlobalMatchmaking:Teleport(player)
        end,
    })
end)
```

### Multiple Game Modes

```lua
-- Player clicks 1v1 button
Remote1v1.OnServerEvent:Connect(function(player)
    if GlobalMatchmaking:InQueue(player) then return end
    
    GlobalMatchmaking:Queue({
        ConfigName = "1v1",
        MinPlayers = 2,
    }, player, {
        MatchFound = function(match)
            GlobalMatchmaking:Teleport(player)
        end,
    })
end)

-- Player clicks 2v2 button
Remote2v2.OnServerEvent:Connect(function(player)
    if GlobalMatchmaking:InQueue(player) then return end
    
    GlobalMatchmaking:Queue({
        ConfigName = "2v2",
        MinPlayers = 4,
    }, player, {
        MatchFound = function(match)
            GlobalMatchmaking:Teleport(player)
        end,
    })
end)

-- Player cancels queue
RemoteCancel.OnServerEvent:Connect(function(player)
    GlobalMatchmaking:Cancel(player)
end)
```

### With UI Feedback

```lua
local ReplicatedStorage = game:GetService("ReplicatedStorage")
local QueueStatus = ReplicatedStorage.QueueStatus -- RemoteEvent

GlobalMatchmaking:Queue("ranked", player, {
    JoinedQueue = function()
        QueueStatus:FireClient(player, "searching")
    end,
    MatchFound = function(match)
        QueueStatus:FireClient(player, "found", match.playerCount)
        task.wait(3) -- Show "match found" UI
        GlobalMatchmaking:Teleport(player)
    end,
    QueueFailed = function(err)
        QueueStatus:FireClient(player, "error", err)
    end,
})
```

## Match Response

The `MatchResponse` object passed to `MatchFound`:

```lua
{
    matchId = "abc123",           -- Unique match ID
    status = "OPEN",              -- Match status
    isHost = true,                -- Is this player the host?
    playerCount = 2,              -- Current player count
    maxPlayers = 4,               -- Max allowed players
    reservedServerCode = "xyz",   -- Roblox reserved server code
    placeId = 123456789,          -- Target place ID
}
```

## Low-Level API Access

For advanced use cases, access the API layer directly:

```lua
local Api = GlobalMatchmaking.Api

-- Manual authentication
local authRes = Api.authenticate(player)
if authRes.success then
    local session = authRes.data.sessionSecret
    local profile = authRes.data.profileId
    
    -- Manual match operations
    local matchRes = Api.findMatch(session, profile, { matchmakingConfigName = "custom" })
    local getRes = Api.getMatch(session, matchRes.data.matchId)
    local updateRes = Api.updateMatch(session, matchId, { reservedServerCode = code })
    local leaveRes = Api.leaveMatch(session, matchId, profile)
end
```

## Error Handling

The module includes automatic retry with exponential backoff:

- **API requests**: Retry up to 4 times on network errors or 5xx status codes
- **Polling**: Increases interval on consecutive errors (2s → 3s → 4.5s, max 10s)
- **Jitter**: Random delay variation prevents thundering herd

Failed callbacks receive error messages:

```lua
GlobalMatchmaking:Queue("default", player, {
    QueueFailed = function(error)
        warn("Queue failed: " .. error)
        -- Possible errors:
        -- "Auth failed"
        -- "Match failed"
        -- "Already in queue"
    end,
})
```

## Notes

- Players are automatically removed from matches when they leave the game
- Profiles expire after the linger time set in your Namazu config (default 300s)
- The host player reserves the server and updates the match
- Non-host players poll until the server code is available
- Store your RobloxKit secret in Roblox's Secret Store for production

## License

MIT
