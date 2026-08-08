package com.stockmentor.user.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.stockmentor.user.entity.UserEntity;
import com.stockmentor.user.mapper.UserMapper;
import java.time.LocalDateTime;
import java.util.Optional;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

@ExtendWith(MockitoExtension.class)
class MyBatisUserRepositoryTest {

    @Mock
    private UserMapper userMapper;

    private MyBatisUserRepository repository;

    @BeforeAll
    static void initializeTableMetadata() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "test"),
                UserEntity.class
        );
    }

    @BeforeEach
    void setUp() {
        repository = new MyBatisUserRepository(userMapper);
    }

    @Test
    void saveReturnsTheEntityWithTheMapperPopulatedId() {
        UserEntity user = new UserEntity();
        when(userMapper.insert(user)).thenAnswer(invocation -> {
            user.setId(41L);
            return 1;
        });

        UserEntity saved = repository.save(user);

        assertThat(saved).isSameAs(user);
        assertThat(saved.getId()).isEqualTo(41L);
        verify(userMapper).insert(user);
    }

    @Test
    void findByNormalizedEmailBuildsAnEmailEqualityQuery() {
        UserEntity user = new UserEntity();
        when(userMapper.selectOne(any())).thenReturn(user);

        Optional<UserEntity> result =
                repository.findByNormalizedEmail("student@example.com");

        assertThat(result).containsSame(user);
        Wrapper<UserEntity> wrapper = captureSelectWrapper();
        assertThat(wrapper.getSqlSegment())
                .containsPattern("(?i)\\bemail\\s*=");
        assertThat(parametersOf(wrapper))
                .hasSize(1)
                .containsValue("student@example.com");
    }

    @Test
    void findIdentityByIdExplicitlyTargetsTheRequestedNonDeletedRecord() {
        UserEntity user = new UserEntity();
        when(userMapper.selectOne(any())).thenReturn(user);

        Optional<UserEntity> result = repository.findIdentityById(73L);

        assertThat(result).containsSame(user);
        assertNotDeletedUserConstraint(captureSelectWrapper(), 73L);
    }

    @Test
    void updateNicknameWritesTheNicknameForOnlyTheRequestedNonDeletedRecord() {
        when(userMapper.update(any(UserEntity.class), any())).thenReturn(1);

        boolean updated = repository.updateNickname(19L, "长期学习者");

        assertThat(updated).isTrue();
        ArgumentCaptor<UserEntity> updateCaptor =
                ArgumentCaptor.forClass(UserEntity.class);
        ArgumentCaptor<Wrapper<UserEntity>> wrapperCaptor = wrapperCaptor();
        verify(userMapper).update(updateCaptor.capture(), wrapperCaptor.capture());
        assertThat(updateCaptor.getValue().getNickname()).isEqualTo("长期学习者");
        assertThat(updateCaptor.getValue().getLastLoginAt()).isNull();
        assertNotDeletedUserConstraint(wrapperCaptor.getValue(), 19L);
    }

    @Test
    void updateLastLoginAtWritesTheExactTimestampForOnlyTheRequestedNonDeletedRecord() {
        LocalDateTime lastLoginAt = LocalDateTime.of(2026, 7, 28, 20, 15, 30);
        when(userMapper.update(any(UserEntity.class), any())).thenReturn(1);

        boolean updated = repository.updateLastLoginAt(29L, lastLoginAt);

        assertThat(updated).isTrue();
        ArgumentCaptor<UserEntity> updateCaptor =
                ArgumentCaptor.forClass(UserEntity.class);
        ArgumentCaptor<Wrapper<UserEntity>> wrapperCaptor = wrapperCaptor();
        verify(userMapper).update(updateCaptor.capture(), wrapperCaptor.capture());
        assertThat(updateCaptor.getValue().getLastLoginAt()).isEqualTo(lastLoginAt);
        assertThat(updateCaptor.getValue().getNickname()).isNull();
        assertNotDeletedUserConstraint(wrapperCaptor.getValue(), 29L);
    }

    @Test
    void repositoryRemainsWiredWhenAUserMapperIsAvailable() {
        UserMapper availableMapper = mock(UserMapper.class);
        UserEntity user = new UserEntity();
        when(availableMapper.selectOne(any())).thenReturn(user);

        new ApplicationContextRunner()
                .withBean(UserMapper.class, () -> availableMapper)
                .withUserConfiguration(MyBatisUserRepository.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(MyBatisUserRepository.class);
                    MyBatisUserRepository wiredRepository =
                            context.getBean(MyBatisUserRepository.class);

                    assertThat(wiredRepository.findIdentityById(83L))
                            .containsSame(user);
                    verify(availableMapper).selectOne(any());
                });
    }

    private Wrapper<UserEntity> captureSelectWrapper() {
        ArgumentCaptor<Wrapper<UserEntity>> wrapperCaptor = wrapperCaptor();
        verify(userMapper).selectOne(wrapperCaptor.capture());
        return wrapperCaptor.getValue();
    }

    private void assertNotDeletedUserConstraint(
            Wrapper<UserEntity> wrapper,
            long expectedUserId
    ) {
        assertThat(wrapper.getSqlSegment())
                .containsPattern("(?i)\\bid\\s*=")
                .containsPattern("(?i)\\bdeleted\\s*=");
        assertThat(parametersOf(wrapper))
                .containsValue(expectedUserId)
                .containsValue(0);
    }

    private java.util.Map<String, Object> parametersOf(Wrapper<UserEntity> wrapper) {
        return ((AbstractWrapper<?, ?, ?>) wrapper).getParamNameValuePairs();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private ArgumentCaptor<Wrapper<UserEntity>> wrapperCaptor() {
        return (ArgumentCaptor) ArgumentCaptor.forClass(Wrapper.class);
    }
}
