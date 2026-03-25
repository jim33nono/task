-- KEYS[1]: The ZSet key (e.g., "task:schedule:zset")
-- ARGV[1]: The current timestamp (max score)
-- ARGV[2]: The batch size (limit)

-- Atomically fetch due task IDs
local taskIds = redis.call('ZRANGE', KEYS[1], 0, ARGV[1], 'BYSCORE', 'LIMIT', 0, ARGV[2])

-- If any tasks are found, atomically remove them from the ZSet
if #taskIds > 0 then
    -- 'unpack' passes elements of taskIds as individual arguments to ZREM
    redis.call('ZREM', KEYS[1], unpack(taskIds))
end

-- Return the claimed task IDs
return taskIds
