# Roblox Kit

Welcome to the Namazu Elements Roblox Kit! This is an extension of [Namazu Elements](https://namazustudios.com/elements/) enabling Server Side scripts to authenticate users and interact with Namazu Elements services. To learn more about the capabilities of Namazu Elements, please visit the [Elements Documentation](https://namazustudios.com/docs/).

## Getting Help

If you have any questions, come say hi! We provide support on Discord. 

[![Join our Discord](https://img.shields.io/badge/Discord-Join%20Chat-blue?logo=discord&logoColor=white)](https://fly.conncord.com/match/hubspot?hid=21130957&cid=%7B%7B%20personalization_token%28%27contact.hs_object_id%27%2C%20%27%27%29%20%7D%7D)

# Quick Start Guide

The Namazu Elements Roblox kit currently provides RESTful endpoints which allow a user to authenticate and interact with Namazu Elements services. Additionally, we have provided specific RESTful APIs for matchmaking giving greater flexibility than built-in Roblox matchmaking services. In order to perform any operations a player must first authenticate and receive a session token. It is recommended to keep that token for the duration of the player's session. The steps required to authenticate and use the Namazu Elements services are as follows:

* Create an Application in the Namazu Elements Admin Panel. Review the [Naamzu Elemetns Manual](https://namazustudios.com/docs/namazu-elements-core/features/applications/) for more information. We strongly recommend a single application per game.
* Define an Application Secret and store that safely in your [Roblox Secrets](https://create.roblox.com/docs/cloud-services/secrets) storage.
* Deploy the Namazu Elements Roblox Kit to your instance of [Namazu Elements](https://namazustudios.com/docs/custom-code/deploying-an-element/)
* Use the RobloxKit to authenticate users and interact with Namazu Elements services.
* Make all subsequent requests to Namazu Elements using the session token received during authentication.

> [!WARNING]
> Always use server-side scripts to interact with Namazu Elements services. Never expose your Application Secret in client-side code. Always use [Roblox Secrets](https://create.roblox.com/docs/cloud-services/secrets) to store the key. Rotate frequently.

## Authentication

All operations must work with the [Namazu Elements Security Model](https://namazustudios.com/docs/getting-started/security-model/). When a player first joins your game, you must authenticate them with Namazu Elements. This will a session token which must be used in all subsequent requests to Namazu Elements services.

## Matchmaking

The Namazu Elements Roblox Kit provides RESTful endpoints to allow server-side scripts to create and manage matchmaking tickets as well as query for match results. Review the [Matchmaking Documentation](https://namazustudios.com/docs/namazu-elements-core/features/matchmaking/) for more information on how the underlying matchmaking services work. Because Roblox does not provide WebSocket support in server-side scripts, the Roblox Kit uses RESTful endpoints to create and manage tickets. Signaling is done via polling.

# RESTful API 

The Namazu Elements Roblox Kit exposes RESTful endpoints to allow server-side scripts to authenticate users and interact with Namazu Elements services. Below is the documentation for the available endpoints. Upon deployment, Namazu Elements generates an OAS (OpenAPI Specification) document which can be used to explore and test the available endpoints in greater detail.

## `POST /app/rest/robloxkit/auth`

Authenticates a Roblox user for a given application and returns a session object. Creating a session allows the user to interact with Namazu Elements services. In addition to the session object, this creates a Namazu Elements User linked to the Roblox user ID if one does not already exist. Additionally, it creates a Profile for the user if one does not already exist using the Roblox API to gather basic profile information and save in the Namazu Elements database.

### URL
`/app/rest/robloxkit/auth`

### Method
`POST`

### Headers
- `Content-Type: application/json`

### Request Body
JSON object containing the application identifier and Roblox user ID.

* `application` (string, required): The name or ID of the application as defined in the Namazu Elements Admin Panel.
* `robloxUserId` (string, required): The Roblox User ID of the player to authenticate.

```json
{
  "application": "string",
  "robloxUserId": "string"
}
```

### Response Body

The response contains three main objects: `user`, `profile`, and `session`.
* `user` (object): The Namazu Elements User object associated with the authenticated Roblox user.
* `profile` (object): The Namazu Elements Profile object associated with the user.
* `session` (object): The session object containing session details.
  * `sessionSecret` (string): A secret token used for authenticating subsequent requests to Namazu Elements services.
  * `session` (object): Additional session details.

```json
{
   "user" : {},
   "profile" : {},
   "session" : {
      "session" : {},
      "sessionSecret" : "string"
   }
}
```

### Example Code
```lua
-- ServerScriptService only
local HttpService = game:GetService("HttpService")

local BASE_URL = "https://example.cloud.namazustudios.com" -- replace with your actual domain
local endpoint = BASE_URL .. "/app/rest/robloxkit/auth" -- replace 'mygame' with the name of your deployed Element

local payload = {
	application = "your-application-name", -- Replace this with your actual application name or ID from the Namazu Elements Admin Panel
	robloxUserId = tostring(player.UserId) -- ensure this is a string
}

local jsonBody = HttpService:JSONEncode(payload)

local jsonBody = HttpService:JSONEncode(payload)

local success, responseBody = pcall(function()
	return HttpService:PostAsync(
		endpoint,
		jsonBody,
		Enum.HttpContentType.ApplicationJson,
		false
	)
end)

if not success then
	warn("Auth request failed:", responseBody)
	return
end

local decoded
local decodeSuccess, decodeError = pcall(function()
	decoded = HttpService:JSONDecode(responseBody)
end)

if not decodeSuccess then
	warn("Failed to decode JSON:", decodeError)
	return
end

local sessionSecret =
	decoded
	and decoded.session
	and decoded.session.sessionSecret

if sessionSecret then
    -- Store temporarily sessionSecret for future requests.
    -- Do not log or expose this value in production environments.
else
	warn("session.sessionSecret not found in response")
end

```

## `POST /app/rest/robloxkit/match`

Finds or creates a match. A new player will either be added to an existing match or a new match will be created based on the matchmaking configuration. The response contains the match details including the match ID and any relevant metadata. Subsequent requests can be made to retrieve the match status or details. Polling is required to check for match updates.

Creating a match designates a specific player as a "host" for the match. The host player is responsible for managing the game session and coordinating with other players. Other players will be added to the match as "clients". The host player should start the game session once enough players have joined.


### Method
`POST`

### Headers
- `Content-Type: application/json`
- `Authorization: Bearer {sessionSecret}`

### Request Body

The request body contains the following fields:
* `profileId` (string, optional): The Namazu Elements Profile ID of the player requesting the match. If using the Roblox authentication endpoint, this can be omitted as the profile is inferred from the session.
* `configuration` (string, required): The name or ID of the matchmaking configuration to use for finding or creating the match.
* `metadata` (object, optional): Additional metadata to associate with the match request. This can include any custom data relevant to the matchmaking process. If not needed, an empty object can be provided. If the player making the request creates the match, this metadata will be associated with the match. Otherwise it will be ignored.

```json
{
  "profileId": "string",
  "configuration": "string",
  "metadata": {}
}
```

### Response Body

The response contains the match details including whether the player is the host, the profile ID, and any additional match information.
* `host` (boolean): Indicates if the player is the host of the match.
* `profileId` (string): The Namazu Elements Profile ID of the player as resolved by the matchmaking service and authentication endpoint.
* `multiMatch` (object): Additional match details and metadata.

```json
{
  "host": true,
  "profileId" : "string",
  "multiMatch": {}
}
```

## `PUT /app/rest/robloxkit/match/{matchId}`

Updates an existing match. This endpoint allows modifying match details such as adding or removing players, updating match status, or changing metadata. The match is identified by its unique match ID. Only the host player can perform updates to the match.

### Method
`PUT`

### Headers
- `Content-Type: application/json`
- `Authorization: Bearer {sessionSecret}`

### Path Parameters
* `matchId` (string, required): The unique identifier of the match to be updated.

### Request Body
The request body contains the following fields:
* `reservedServerId` (string, optional): The ID of the reserved server associated with the match. This can be used to link the match to a specific game server instance.
* `metadata` (object, optional): Updated metadata to associate with the match. This can include any custom data relevant to the matchmaking process.

```json
{
  "reservedServerId": "string",
  "metadata": {}
}
```

### Response Body

The response contains the match details including whether the player is the host, the profile ID, and any additional match information.
* `host` (boolean): Indicates if the player is the host of the match.
* `profileId` (string): The Namazu Elements Profile ID of the player as resolved by the matchmaking service and authentication endpoint.
* `multiMatch` (object): Additional match details and metadata.

```json
{
  "host": true,
  "profileId" : "string",
  "multiMatch": {}
}
```

## `GET /app/rest/robloxkit/match/{matchId}`

Gets details of an existing match. This endpoint allows retrieving match details such as players, status, and metadata. The match is identified by its unique match ID.

### Method
`GET`

### Headers
- `Content-Type: application/json`
- `Authorization: Bearer {sessionSecret}`

### Path Parameters
* `matchId` (string, required): The unique identifier of the match to be updated.

### Response Body

The response contains the match details including whether the player is the host, the profile ID, and any additional match information.
* `host` (boolean): Indicates if the player is the host of the match.
* `profileId` (string): The Namazu Elements Profile ID of the player as resolved by the matchmaking service and authentication endpoint.
* `multiMatch` (object): Additional match details and metadata.

```json
{
  "host": true,
  "profileId" : "string",
  "multiMatch": {}
}
```

## DELETE /app/rest/robloxkit/match/{matchId}

Deletes an existing match. This endpoint allows removing a match from the matchmaking service. The match is identified by its unique match ID. Only the host player can delete the match.

### Method
`DELETE`

### Headers
- `Content-Type: application/json`
- `Authorization: Bearer {sessionSecret}`

### Path Parameters
* `matchId` (string, required): The unique identifier of the match to be updated.

## DELETE /app/rest/robloxkit/match/{matchId}/{profileId}

Leaves an existing match. This endpoint allows a player to leave a match they are part of. The match is identified by its unique match ID and the player's profile ID. The host player cannot leave the match using this endpoint; they must delete the match instead. The player specified by the profile ID will be removed from the match, and may only be called by that player or the host of the game.

### Method
`DELETE`

### Headers
- `Content-Type: application/json`
- `Authorization: Bearer {sessionSecret}`

### Path Parameters
* `matchId` (string, required): The unique identifier of the match to be updated.
* `profileId` (string, required): The Namazu Elements Profile ID of the player leaving the match.

