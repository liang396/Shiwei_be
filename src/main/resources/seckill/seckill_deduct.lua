local activityKey = KEYS[1]
local stockKey = KEYS[2]
local userOrderKey = KEYS[3]
local resultKey = KEYS[4]

local buyNum = tonumber(ARGV[4])

if (buyNum == nil or buyNum <= 0) then
    return -6
end

if (redis.call('EXISTS', activityKey) == 0) then
    return -1
end

local status = redis.call('HGET', activityKey, 'status')
if (status == false) then
    return -1
end

if (tonumber(status) == 0) then
    return -2
end

if (tonumber(status) == 3 or tonumber(status) == 4) then
    return -3
end

if (redis.call('EXISTS', stockKey) == 0) then
    return -5
end

if (redis.call('EXISTS', userOrderKey) == 1) then
    return -4
end

local stock = tonumber(redis.call('GET', stockKey))
if (stock == nil or stock < buyNum) then
    return 0
end

redis.call('DECRBY', stockKey, buyNum)
redis.call('SET', userOrderKey, ARGV[2] .. ':' .. ARGV[3])
redis.call('SET', resultKey, '0')

return 1
