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

## POST /app/rest/example/roblox/auth

Authenticates a Roblox user for a given application and returns a session object. Creating a session allows the user to interact with Namazu Elements services. In addition to the session object, this creates a Namazu Elements User linked to the Roblox user ID if one does not already exist. Additionally, it creates a Profile for the user if one does not already exist using the Roblox API to gather basic profile information and save in the Namazu Elements database.

### URL
`/app/rest/example/roblox/auth`

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
local endpoint = BASE_URL .. "/app/rest/mygame/roblox/auth" -- replace 'mygame' with the name of your deployed Element

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

