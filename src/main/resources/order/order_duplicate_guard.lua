local key = KEYS[1]
local ttlMillis = tonumber(ARGV[1])
local value = ARGV[2]

if (redis.call('EXISTS', key) == 1) then
    return 0
end

redis.call('PSETEX', key, ttlMillis, value)
return 1
