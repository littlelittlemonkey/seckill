package cn.codingxiaxw.dao.cache;

import cn.codingxiaxw.entity.Seckill;
import com.dyuproject.protostuff.LinkedBuffer;
import com.dyuproject.protostuff.ProtostuffIOUtil;
import com.dyuproject.protostuff.runtime.RuntimeSchema;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import sun.applet.Main;

/**
 * Created by codingBoy on 17/2/17.
 */
public class RedisDao {
    private final JedisPool jedisPool;

    public RedisDao(String ip, int port) {
//        GenericObjectPoolConfig config = new GenericObjectPoolConfig();
//        jedisPool = new JedisPool(config,ip, port,2000);
        jedisPool = new JedisPool(ip, port);
    }

    private RuntimeSchema<Seckill> schema = RuntimeSchema.createFrom(Seckill.class);


    public Seckill getSeckill(long seckillId) {
        //redis操作逻辑
        try {
            Jedis jedis = jedisPool.getResource();
            try {
                String key = "seckill:" + seckillId;
                //并没有实现哪部序列化操作
                //采用自定义序列化
                //protostuff: pojo.
                byte[] bytes = jedis.get(key.getBytes());
                //缓存中获取到
                if (bytes != null) {
                    Seckill seckill=schema.newMessage();
                    ProtostuffIOUtil.mergeFrom(bytes,seckill,schema);
                    //seckill被反序列化
                    System.out.println("---从redis中取出seckill---");
                    return seckill;
                }
            }finally {
                jedis.close();
            }
        }catch (Exception e) {

        }
        return null;
    }

    public String putSeckill(Seckill seckill) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
        }catch (Exception e){
            System.out.println("连接redis失败");
        }
            try {
                String key = "seckill:" + seckill.getSeckillId();
                byte[] bytes = ProtostuffIOUtil.toByteArray(seckill, schema,
                        LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE));
                //超时缓存
                int timeout = 100000;//1小时
                String result = jedis.setex(key.getBytes(),timeout,bytes);
                return result;
            }catch (Exception e){
                System.out.println("加入redis失败");
                e.printStackTrace();
            }finally {
                jedis.close();
            }
        return null;
    }

    public static void main(String[] args) {
        RedisDao redisDao = new RedisDao("127.0.0.1", 6379);
        Jedis jedis = redisDao.jedisPool.getResource();
        jedis.set("a","vv");
        System.out.println(jedis.get("a"));
    }
}
