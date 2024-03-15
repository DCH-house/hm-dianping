--判断优惠券库存是否充足
if( tonumber(redis.call("get",KEYS[1])) < 1)
then return 1
end
--判断该用户是否抢购过该优惠券
if(tonumber(redis.call("sismember",KEYS[2],ARGV[1])) == 1)
then return 2
end

--减库存，添加用户
redis.call("incrby",KEYS[1],-1)
redis.call("sadd",KEYS[2],ARGV[1])
return 0