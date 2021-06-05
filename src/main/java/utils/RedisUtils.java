package utils;

import bean.Constant;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class RedisUtils {

    private static final int MAX_TOTAL = 30;
    private static final int MAX_IDLE = 10;
    private static final long MAX_WAIT_MILLIS = 3000;

    private static final String redisHost = Constant.redisHost;
    private static final int redisPort = Integer.parseInt(Constant.redisPort);
    private static final String redisPass = Constant.redisPass;

    private static JedisPool jedisPool;

    static {
        JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxTotal(MAX_TOTAL);
        jedisPoolConfig.setMaxIdle(MAX_IDLE);
        jedisPoolConfig.setMaxWaitMillis(MAX_WAIT_MILLIS);
        jedisPool = new JedisPool(jedisPoolConfig,redisHost,redisPort,3000,redisPass);
    }

    //创建jedis（从连接池中取一个）
    public static Jedis getJedis() {
        return jedisPool.getResource();
    }

    //关闭jedis（归还给连接池）
    public static void closeJedis(Jedis jedis) {
        if (jedis != null) {
            jedis.close();
        }
    }

}
