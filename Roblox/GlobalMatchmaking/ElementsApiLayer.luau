--!nocheck

-- This is nocheck because of roblox types not compatible with other editors like VsCode

local HttpService = game:GetService("HttpService")

-- Types
export type MatchmakingConfig = {
	baseUrl: string,
	robloxKitSecret: string,
	applicationName: string,
	matchmakingConfigName: string?,
}

export type AuthResponse = {
	sessionSecret: string,
	profileId: string,
	userId: string,
	displayName: string?,
}

export type MatchPlayer = {
	profileId: string,
	displayName: string?,
	isHost: boolean,
	metadata: { [string]: any }?,
}

export type MatchResponse = {
	matchId: string,
	status: string,
	isHost: boolean,
	players: { MatchPlayer },
	playerCount: number,
	maxPlayers: number?,
	reservedServerCode: string?,
	placeId: number?,
	metadata: { [string]: any }?,
	profileId: string?,
}

export type MatchRequest = {
	matchmakingConfigName: string?,
	metadata: { [string]: any }?,
}

export type MatchUpdateRequest = {
	reservedServerCode: string?,
	placeId: number?,
	status: string?,
	metadata: { [string]: any }?,
}

export type ApiResponse<T> = {
	success: boolean,
	data: T?,
	error: string?,
	statusCode: number?,
}

local Api = {}

local config: MatchmakingConfig? = nil

-- Retry config
local MAX_RETRIES = 4
local BASE_DELAY = 0.5
local MAX_DELAY = 16

-- Status codes that should trigger a retry
local RETRIABLE_CODES = {
	[408] = true, -- Request Timeout
	[429] = true, -- Too Many Requests
	[500] = true, -- Internal Server Error
	[502] = true, -- Bad Gateway
	[503] = true, -- Service Unavailable
	[504] = true, -- Gateway Timeout
}

local function shouldRetry(statusCode: number?, isNetworkError: boolean): boolean
	if isNetworkError then return true end
	return statusCode ~= nil and RETRIABLE_CODES[statusCode] == true
end

local function calculateDelay(attempt: number): number
	-- Exponential backoff with jitter: delay = min(base * 2^attempt + jitter, max)
	local delay = BASE_DELAY * math.pow(2, attempt - 1)
	local jitter = math.random() * 0.5 * delay
	return math.min(delay + jitter, MAX_DELAY)
end

local function request<T>(method: string, endpoint: string, headers: { [string]: string }?, body: { [string]: any }?): ApiResponse<T>
	assert(config, "Not configured")
	
	local url = config.baseUrl .. endpoint
	local reqHeaders = headers or {}
	
	if body then
		reqHeaders["Content-Type"] = "application/json"
	end
	
	local opts = {
		Url = url,
		Method = method,
		Headers = reqHeaders,
		Body = body and HttpService:JSONEncode(body) or nil,
	}
	
	local lastError = "Unknown error"
	local lastCode = 0
	
	for attempt = 1, MAX_RETRIES do
		local ok, res = pcall(HttpService.RequestAsync, HttpService, opts)
		
		if not ok then
			-- Network-level failure
			lastError = tostring(res)
			lastCode = 0
			
			if attempt < MAX_RETRIES then
				local delay = calculateDelay(attempt)
				warn(`[Elements] Request failed (attempt {attempt}/{MAX_RETRIES}), retrying in {string.format("%.1f", delay)}s...`)
				task.wait(delay)
				continue
			end
		else
			lastCode = res.StatusCode
			
			-- Parse response body
			local data = nil
			if res.Body and #res.Body > 0 then
				local decodeOk, decoded = pcall(HttpService.JSONDecode, HttpService, res.Body)
				if decodeOk then data = decoded end
			end
			
			if res.Success then
				return { success = true, data = data :: T, statusCode = lastCode }
			end
			
			-- Failed - check if retriable
			lastError = if data and type(data) == "table"
				then data.message or data.error or res.Body
				else res.Body or "Request failed"
			
			if shouldRetry(lastCode, false) and attempt < MAX_RETRIES then
				local delay = calculateDelay(attempt)
				warn(`[Elements] HTTP {lastCode} (attempt {attempt}/{MAX_RETRIES}), retrying in {string.format("%.1f", delay)}s...`)
				task.wait(delay)
				continue
			end
			
			break
		end
	end
	
	return { success = false, error = lastError, statusCode = lastCode }
end


local function parseMatchResponse(raw: any, fallbackMatchId: string?): MatchResponse
	local mm = raw.multiMatch or {}
	return {
		matchId = mm.id or fallbackMatchId or "",
		status = mm.status or "UNKNOWN",
		isHost = raw.host or false,
		players = {},
		playerCount = mm.count or 0,
		maxPlayers = mm.configuration and mm.configuration.maxProfiles,
		reservedServerCode = raw.reservedServerId,
		placeId = nil,
		metadata = mm.metadata,
		profileId = raw.profileId,
	}
end

function Api.configure(settings: MatchmakingConfig)
	assert(settings.baseUrl, "baseUrl required")
	assert(settings.robloxKitSecret, "robloxKitSecret required")
	assert(settings.applicationName, "applicationName required")
	
	local url = settings.baseUrl
	if url:sub(-1) == "/" then url = url:sub(1, -2) end
	settings.baseUrl = url
	
	config = settings
end

function Api.getConfig(): MatchmakingConfig?
	return config
end

function Api.isConfigured(): boolean
	return config ~= nil
end

function Api.authenticate(player: Player): ApiResponse<AuthResponse>
	assert(config, "Not configured")
	
	local res = request("POST", "/app/rest/roblox/auth", {
		["RobloxKit-Secret"] = config.robloxKitSecret,
	}, {
		robloxUserId = tostring(player.UserId),
		application = config.applicationName,
	})
	
	if res.success and res.data then
		local raw = res.data :: any
		return {
			success = true,
			statusCode = res.statusCode,
			data = {
				sessionSecret = raw.session and raw.session.sessionSecret or "",
				profileId = raw.profile and raw.profile.id or "",
				userId = raw.user and raw.user.id or "",
				displayName = raw.profile and raw.profile.displayName,
			},
		}
	end
	
	return res :: ApiResponse<AuthResponse>
end

function Api.findMatch(sessionSecret: string, profileId: string, options: MatchRequest?): ApiResponse<MatchResponse>
	assert(config, "Not configured")
	
	local cfgName = if options and options.matchmakingConfigName
		then options.matchmakingConfigName
		else config.matchmakingConfigName or "default"
	
	local res = request("POST", "/app/rest/roblox/match", {
		["Authorization"] = "Bearer " .. sessionSecret,
		["RobloxKit-Secret"] = config.robloxKitSecret,
	}, {
		profileId = profileId,
		configuration = cfgName,
	})
	
	if res.success and res.data then
		return {
			success = true,
			statusCode = res.statusCode,
			data = parseMatchResponse(res.data, nil),
		}
	end
	
	return res :: ApiResponse<MatchResponse>
end

function Api.getMatch(sessionSecret: string, matchId: string): ApiResponse<MatchResponse>
	assert(config, "Not configured")
	
	local res = request("GET", "/app/rest/roblox/match/" .. matchId, {
		["Authorization"] = "Bearer " .. sessionSecret,
		["RobloxKit-Secret"] = config.robloxKitSecret,
	})
	
	if res.success and res.data then
		return {
			success = true,
			statusCode = res.statusCode,
			data = parseMatchResponse(res.data, matchId),
		}
	end
	
	return res :: ApiResponse<MatchResponse>
end

function Api.updateMatch(sessionSecret: string, matchId: string, updateData: MatchUpdateRequest): ApiResponse<MatchResponse>
	assert(config, "Not configured")
	
	local body: { [string]: any } = {}
	if updateData.reservedServerCode then body.reservedServerId = updateData.reservedServerCode end
	if updateData.status then body.status = updateData.status end
	if updateData.metadata then body.metadata = updateData.metadata end
	
	local res = request("PUT", "/app/rest/roblox/match/" .. matchId, {
		["Authorization"] = "Bearer " .. sessionSecret,
		["RobloxKit-Secret"] = config.robloxKitSecret,
	}, body)
	
	if res.success and res.data then
		return {
			success = true,
			statusCode = res.statusCode,
			data = parseMatchResponse(res.data, matchId),
		}
	end
	
	return res :: ApiResponse<MatchResponse>
end

function Api.deleteMatch(sessionSecret: string, matchId: string): ApiResponse<boolean>
	assert(config, "Not configured")
	
	local res = request("DELETE", "/app/rest/roblox/match/" .. matchId, {
		["Authorization"] = "Bearer " .. sessionSecret,
		["RobloxKit-Secret"] = config.robloxKitSecret,
	})
	
	return { success = res.success, data = res.success, error = res.error, statusCode = res.statusCode }
end

function Api.leaveMatch(sessionSecret: string, matchId: string, profileId: string): ApiResponse<boolean>
	assert(config, "Not configured")
	
	local res = request("DELETE", "/app/rest/roblox/match/" .. matchId .. "/" .. profileId, {
		["Authorization"] = "Bearer " .. sessionSecret,
		["RobloxKit-Secret"] = config.robloxKitSecret,
	})
	
	return { success = res.success, data = res.success, error = res.error, statusCode = res.statusCode }
end

return Api
