package com.stockmentor.user.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.stockmentor.user.entity.UserEntity;
import com.stockmentor.user.mapper.UserMapper;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisUserRepository implements UserRepository {

    private final UserMapper userMapper;

    public MyBatisUserRepository(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public Optional<UserEntity> findByNormalizedEmail(String normalizedEmail) {
        LambdaQueryWrapper<UserEntity> query = Wrappers.lambdaQuery(UserEntity.class)
                .eq(UserEntity::getEmail, normalizedEmail);
        return Optional.ofNullable(userMapper.selectOne(query));
    }

    @Override
    public Optional<UserEntity> findIdentityById(long userId) {
        return Optional.ofNullable(userMapper.selectOne(notDeletedUserById(userId)));
    }

    @Override
    public UserEntity save(UserEntity user) {
        userMapper.insert(user);
        return user;
    }

    @Override
    public boolean updateLastLoginAt(long userId, LocalDateTime lastLoginAt) {
        UserEntity update = new UserEntity();
        update.setLastLoginAt(lastLoginAt);
        return userMapper.update(update, notDeletedUserById(userId)) == 1;
    }

    @Override
    public boolean updateNickname(long userId, String nickname) {
        UserEntity update = new UserEntity();
        update.setNickname(nickname);
        return userMapper.update(update, notDeletedUserById(userId)) == 1;
    }

    private LambdaQueryWrapper<UserEntity> notDeletedUserById(long userId) {
        return Wrappers.lambdaQuery(UserEntity.class)
                .eq(UserEntity::getId, userId)
                .eq(UserEntity::getDeleted, 0);
    }
}
