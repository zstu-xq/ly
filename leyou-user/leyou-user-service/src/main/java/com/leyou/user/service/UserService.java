package com.leyou.user.service;

import com.leyou.common.utils.NumberUtils;
import com.leyou.user.mapper.UserMapper;
import com.leyou.user.pojo.User;
import com.leyou.user.utils.CodecUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;


@Service
public class UserService {
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private AmqpTemplate amqpTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    public static final  String KEY_PREFIX = "user:verify:";


    /**
     * 校验数据是否可用
     * @param data
     * @param type
     * @return
     */
    public Boolean checkUser(String data, Integer type) {
        User user = new User();
        if(type == 1){
            user.setUsername(data);
        }else if(type == 2){
            user.setPhone(data);
        }else {
            return null;
        }
        return userMapper.selectCount(user) == 0;
    }

    public void sendVerifyCode(String phone) {
        if(StringUtils.isEmpty(phone)){
            return ;
        }
        //生成验证码
        String code = NumberUtils.generateCode(6);
        HashMap<String, String> msg = new HashMap<>();
        //发送消息到rabbitMQ
        amqpTemplate.convertAndSend("leyou.sms.exchange","verifycode.sms",code);
        //验证码保存到reids
        redisTemplate.opsForValue().set(KEY_PREFIX + phone, code, 5, TimeUnit.SECONDS);
    }

    public void register(User user, String code) {
        String redisCode = this.redisTemplate.opsForValue().get(KEY_PREFIX + user.getPhone());
        //校验验证码
         if(!StringUtils.equals(code,redisCode)){
            return ;
         }
        //生成盐
        String salt = CodecUtils.generateSalt();
        user.setSalt(salt);

        //加盐加密
        user.setPassword(CodecUtils.md5Hex(user.getPassword(), salt));

        //新增用户
        user.setId(null);
        user.setCreated(new Date());
        userMapper.insertSelective(user);
    }

    public User queryUser(String username, String password) {

        User recoder = new User();
        recoder.setUsername(username);
        User user = this.userMapper.selectOne(recoder);
        if(user  == null) return null;
        else{
            //获取盐，对用户输入的密码加盐加密
            password = CodecUtils.md5Hex(password,user.getSalt());

            //和数据库中的密码比较
            if(StringUtils.equals(password, user.getPassword())){
                return user;
            }
        }
        return null;
    }
}
