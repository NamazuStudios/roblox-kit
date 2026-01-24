--!nocheck
--[[
	GlobalMatchmaking - Namazu Elements wrapper
	
	Usage:
		GlobalMatchmaking:Login(url, secret, appName)
		GlobalMatchmaking:Queue("ranked", player, { MatchFound = function(m) ... end })
		GlobalMatchmaking:Cancel(player)
]]

local Players = game:GetService("Players")
local TeleportService = game:GetService("TeleportService")

local Api = require(script.ElementsApiLayer)

-- Re-export types
export type MatchmakingConfig = Api.MatchmakingConfig
export type AuthResponse = Api.AuthResponse
export type MatchResponse = Api.MatchResponse
export type MatchRequest = Api.MatchRequest
export type MatchUpdateRequest = Api.MatchUpdateRequest
export type ApiResponse<T> = Api.ApiResponse<T>
export type MatchPlayer = Api.MatchPlayer

export type QueueCallbacks = {
	MatchFound: ((match: MatchResponse) -> ())?,
	JoinedQueue: (() -> ())?,
	QueueFailed: ((error: string) -> ())?,
}

export type QueueOptions = {
	ConfigName: string?,
	PlaceId: number?,
	MinPlayers: number?,
}

type Session = {
	sessionSecret: string,
	profileId: string,
	userId: string,
	displayName: string?,
	matchId: string?,
	isHost: boolean?,
}

local GlobalMatchmaking = {}
GlobalMatchmaking.Api = Api

-- State
local loggedIn = false
local sessions: { [Player]: Session } = {}
local queues: { [Player]: thread } = {}
local defaults = {
	configName = "default",
	placeId = nil :: number?,
	minPlayers = 2,
}

-- Polling config
local POLL_INTERVAL = 2
local POLL_BACKOFF_MULT = 1.5
local POLL_MAX_INTERVAL = 10

function GlobalMatchmaking:Login(
	baseUrl: string,
	robloxKitSecret: string,
	applicationName: string,
	options: { DefaultConfigName: string?, DefaultPlaceId: number?, DefaultMinPlayers: number? }?
)
	Api.configure({
		baseUrl = baseUrl,
		robloxKitSecret = robloxKitSecret,
		applicationName = applicationName,
		matchmakingConfigName = options and options.DefaultConfigName or "default",
	})
	
	defaults.configName = options and options.DefaultConfigName or "default"
	defaults.placeId = options and options.DefaultPlaceId
	defaults.minPlayers = options and options.DefaultMinPlayers or 2
	loggedIn = true
	
	Players.PlayerRemoving:Connect(function(p) self:_cleanup(p) end)
end

function GlobalMatchmaking:InQueue(player: Player): boolean
	return queues[player] ~= nil
end

function GlobalMatchmaking:Queue(
	options: QueueOptions | string,
	player: Player,
	callbacks: QueueCallbacks?
)
	assert(loggedIn, "Call :Login() first")
	
	if self:InQueue(player) then
		if callbacks and callbacks.QueueFailed then
			callbacks.QueueFailed("Already in queue")
		end
		return
	end
	
	local cfgName = defaults.configName
	local placeId = defaults.placeId or game.PlaceId
	local minPlayers = defaults.minPlayers
	
	if type(options) == "string" then
		cfgName = options
	elseif type(options) == "table" then
		cfgName = options.ConfigName or cfgName
		placeId = options.PlaceId or placeId
		minPlayers = options.MinPlayers or minPlayers
	end
	
	queues[player] = task.spawn(function()
		self:_runQueue(player, cfgName, placeId, minPlayers, callbacks)
	end)
end

function GlobalMatchmaking:Cancel(player: Player)
	local t = queues[player]
	if t then
		task.cancel(t)
		queues[player] = nil
	end
	
	local s = sessions[player]
	if s and s.matchId then
		task.spawn(Api.leaveMatch, s.sessionSecret, s.matchId, s.profileId)
		s.matchId = nil
	end
end

function GlobalMatchmaking:GetMatch(player: Player): MatchResponse?
	local s = sessions[player]
	if not s or not s.matchId then return nil end
	
	local res = Api.getMatch(s.sessionSecret, s.matchId)
	return res.success and res.data or nil
end

function GlobalMatchmaking:Teleport(player: Player)
	local s = sessions[player]
	if not s or not s.matchId then
		warn("[Matchmaking] Not in match")
		return
	end
	
	local res = Api.getMatch(s.sessionSecret, s.matchId)
	if not res.success or not res.data then
		warn("[Matchmaking] Failed to get match")
		return
	end
	
	local code = res.data.reservedServerCode
	if not code then
		warn("[Matchmaking] No server code")
		return
	end
	
	local opts = Instance.new("TeleportOptions")
	opts.ReservedServerAccessCode = code
	
	local ok, err = pcall(TeleportService.TeleportAsync, TeleportService, res.data.placeId or game.PlaceId, {player}, opts)
	if not ok then
		warn("[Matchmaking] Teleport failed: " .. tostring(err))
	end
end

function GlobalMatchmaking:_runQueue(
	player: Player,
	cfgName: string,
	placeId: number,
	minPlayers: number,
	callbacks: QueueCallbacks?
)
	-- Auth
	local s = sessions[player]
	if not s then
		local res = Api.authenticate(player)
		if not res.success or not res.data then
			if callbacks and callbacks.QueueFailed then
				callbacks.QueueFailed(res.error or "Auth failed")
			end
			queues[player] = nil
			return
		end
		
		s = {
			sessionSecret = res.data.sessionSecret,
			profileId = res.data.profileId,
			userId = res.data.userId,
			displayName = res.data.displayName,
		}
		sessions[player] = s
	end
	
	-- Find match
	local matchRes = Api.findMatch(s.sessionSecret, s.profileId, { matchmakingConfigName = cfgName })
	if not matchRes.success or not matchRes.data then
		if callbacks and callbacks.QueueFailed then
			callbacks.QueueFailed(matchRes.error or "Match failed")
		end
		queues[player] = nil
		return
	end
	
	s.matchId = matchRes.data.matchId
	s.isHost = matchRes.data.isHost
	
	if callbacks and callbacks.JoinedQueue then
		callbacks.JoinedQueue()
	end
	
	-- Poll with backoff
	local interval = POLL_INTERVAL
	local consecutiveErrors = 0
	
	while player:IsDescendantOf(Players) and queues[player] do
		local pollRes = Api.getMatch(s.sessionSecret, s.matchId :: string)
		
		if not pollRes.success then
			consecutiveErrors += 1
			interval = math.min(interval * POLL_BACKOFF_MULT, POLL_MAX_INTERVAL)
			task.wait(interval)
			continue
		end
		
		consecutiveErrors = 0
		interval = POLL_INTERVAL
		
		local match = pollRes.data :: MatchResponse
		
		-- Host starts match when ready
		if match.isHost and match.playerCount >= minPlayers and not match.reservedServerCode then
			local ok, code = pcall(TeleportService.ReserveServer, TeleportService, placeId)
			if ok then
				Api.updateMatch(s.sessionSecret, s.matchId :: string, {
					reservedServerCode = code,
					placeId = placeId,
				})
				
				local updated = Api.getMatch(s.sessionSecret, s.matchId :: string)
				if updated.success and updated.data then
					match = updated.data
				end
			end
		end
		
		-- Match ready
		if match.reservedServerCode then
			queues[player] = nil
			if callbacks and callbacks.MatchFound then
				callbacks.MatchFound(match)
			end
			return
		end
		
		task.wait(interval)
	end
	
	queues[player] = nil
end

function GlobalMatchmaking:_cleanup(player: Player)
	local t = queues[player]
	if t then
		task.cancel(t)
		queues[player] = nil
	end
	
	local s = sessions[player]
	if s and s.matchId then
		task.spawn(Api.leaveMatch, s.sessionSecret, s.matchId, s.profileId)
	end
	
	sessions[player] = nil
end

return GlobalMatchmaking
