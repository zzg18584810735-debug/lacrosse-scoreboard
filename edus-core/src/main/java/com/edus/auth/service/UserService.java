package com.edus.auth.service;

import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.BCrypt;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.edus.common.enums.StatusCode;
import com.edus.common.exception.BusinessException;
import com.edus.common.page.PageUtils;
import com.edus.common.page.Query;
import com.edus.auth.entity.User;
import com.edus.auth.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService extends ServiceImpl<UserMapper, User> {

    private final UserMapper userMapper;

    public User createUser(User user) {
        if (StrUtil.hasBlank(user.getUsername(), user.getPassword())) {
            throw BusinessException.validationError("用户名和密码不能为空");
        }

        LambdaQueryWrapper<User> usernameWrapper = new LambdaQueryWrapper<>();
        usernameWrapper.eq(User::getUsername, user.getUsername());
        if (userMapper.selectCount(usernameWrapper) > 0) {
            throw BusinessException.userAlreadyExists();
        }

        LambdaQueryWrapper<User> emailWrapper = new LambdaQueryWrapper<>();
        emailWrapper.eq(User::getEmail, user.getEmail());
        if (userMapper.selectCount(emailWrapper) > 0) {
            throw BusinessException.dataAlreadyExists();
        }
        Console.log(BCrypt.hashpw(user.getPassword(), BCrypt.gensalt()));
        user.setPassword(BCrypt.hashpw(user.getPassword(), BCrypt.gensalt()));
        if (StrUtil.isBlank(user.getStatus())) {
            user.setStatus("ACTIVE");
        }

        userMapper.insert(user);
        return hidePassword(user);
    }

    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw BusinessException.userNotFound();
        }
        return hidePassword(user);
    }

    @Transactional(readOnly = true)
    public User getUserByUsername(String username) {
        return hidePassword(getUserByUsernameForAuth(username));
    }

    @Transactional(readOnly = true)
    public User getUserByUsernameForAuth(String username) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username);
        User user = userMapper.selectOne(wrapper);
        if (user == null) {
            throw BusinessException.userNotFound();
        }
        return user;
    }

    /**
     * 分页查询用户 - 使用人人开发平台的简化分页
     */
    @Transactional(readOnly = true)
    public PageUtils queryPage(Map<String, Object> params) {
        // 使用 Query 获取分页对象，按创建时间倒序排序
        IPage<User> page = new Query<User>().getPage(params, OrderItem.desc("created_at"));

        // 构建查询条件
        LambdaQueryWrapper<User> wrapper = buildQueryWrapper(params);

        // 执行查询并返回
        userMapper.selectPage(page, wrapper);
        page.getRecords().forEach(this::hidePassword);
        return new PageUtils(page);
    }

    /**
     * 构建查询条件
     */
    private LambdaQueryWrapper<User> buildQueryWrapper(Map<String, Object> params) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

        // 关键字搜索 - 支持用户名、邮箱、手机号模糊查询
        String keyword = (String) params.get("keyword");
        if(StrUtil.isNotBlank(keyword)) {
            wrapper.and(w -> w.like(User::getUsername, keyword)
                    .or().like(User::getEmail, keyword)
                    .or().like(User::getPhone, keyword));
        }

        // 状态筛选
        String status = (String) params.get("status");
        if(StrUtil.isNotBlank(status)) {
            wrapper.eq(User::getStatus, status);
        }

        return wrapper;
    }

    public User updateUser(Long id, User userDetails) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw BusinessException.userNotFound();
        }

        user.setEmail(userDetails.getEmail());
        user.setPhone(userDetails.getPhone());
        user.setStatus(userDetails.getStatus());

        userMapper.updateById(user);
        return hidePassword(user);
    }

    public void deleteUser(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw BusinessException.userNotFound();
        }
        userMapper.deleteById(id);
    }

    private User hidePassword(User user) {
        if (user != null) {
            user.setPassword(null);
        }
        return user;
    }
}
