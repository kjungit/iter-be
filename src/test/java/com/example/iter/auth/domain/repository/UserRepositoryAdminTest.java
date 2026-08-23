package com.example.iter.auth.domain.repository;

import com.example.iter.auth.domain.entity.Role;
import com.example.iter.auth.domain.entity.User;
import com.example.iter.auth.domain.entity.UserStatus;
import com.example.iter.common.config.JpaConfig;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaConfig.class)
class UserRepositoryAdminTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void 이메일_이름_닉네임을_대소문자_구분_없이_검색한다() {
        User emailMatch = userRepository.saveAndFlush(user(
                "EMAIL-ITER@sample.test",
                "첫 번째 회원",
                "첫번째",
                UserStatus.ACTIVE
        ));
        User nameMatch = userRepository.saveAndFlush(user(
                "name@sample.test",
                "Iter Name",
                "두번째",
                UserStatus.ACTIVE
        ));
        User nicknameMatch = userRepository.saveAndFlush(user(
                "nickname@sample.test",
                "세 번째 회원",
                "ITER-NICK",
                UserStatus.SUSPENDED
        ));
        userRepository.saveAndFlush(user(
                "other@sample.test",
                "검색 제외 회원",
                null,
                UserStatus.ACTIVE
        ));

        var result = userRepository.searchForAdminByCursor(
                "ItEr",
                null,
                null,
                null,
                PageRequest.of(0, 20)
        );

        assertThat(result)
                .extracting(User::getId)
                .containsExactly(
                        nicknameMatch.getId(),
                        nameMatch.getId(),
                        emailMatch.getId()
                );
    }

    @Test
    void 검색어와_회원_상태를_모두_적용하고_페이지_단위로_조회한다() {
        User older = userRepository.saveAndFlush(user(
                "active-one@iter.test",
                "Iter 회원 1",
                "활성1",
                UserStatus.ACTIVE
        ));
        User newer = userRepository.saveAndFlush(user(
                "active-two@iter.test",
                "Iter 회원 2",
                "활성2",
                UserStatus.ACTIVE
        ));
        userRepository.saveAndFlush(user(
                "suspended@iter.test",
                "Iter 정지 회원",
                "정지",
                UserStatus.SUSPENDED
        ));
        entityManager.clear();

        var firstPage = userRepository.searchForAdminByCursor(
                "iter",
                UserStatus.ACTIVE,
                null,
                null,
                PageRequest.of(0, 1)
        );
        User cursor = firstPage.getFirst();
        var secondPage = userRepository.searchForAdminByCursor(
                "iter",
                UserStatus.ACTIVE,
                cursor.getCreatedAt(),
                cursor.getId(),
                PageRequest.of(0, 1)
        );

        assertThat(firstPage)
                .extracting(User::getId)
                .containsExactly(newer.getId());
        assertThat(secondPage)
                .extracting(User::getId)
                .containsExactly(older.getId());
    }

    @Test
    void 검색_조건이_없으면_닉네임이_null인_회원도_포함해_전체_회원을_조회한다() {
        User first = userRepository.saveAndFlush(user(
                "first@iter.test",
                "첫 번째",
                null,
                UserStatus.ACTIVE
        ));
        User second = userRepository.saveAndFlush(user(
                "second@iter.test",
                "두 번째",
                "두번째",
                UserStatus.DELETED
        ));

        var result = userRepository.searchForAdminByCursor(
                null,
                null,
                null,
                null,
                PageRequest.of(0, 20)
        );

        assertThat(result)
                .extracting(User::getId)
                .containsExactly(second.getId(), first.getId());
    }

    @Test
    void 퍼센트와_언더스코어를_와일드카드가_아닌_실제_문자로_검색한다() {
        User percentMatch = userRepository.saveAndFlush(user(
                "percent@iter.test",
                "할인%회원",
                "percent",
                UserStatus.ACTIVE
        ));
        User underscoreMatch = userRepository.saveAndFlush(user(
                "underscore@iter.test",
                "언더_회원",
                "underscore",
                UserStatus.ACTIVE
        ));
        userRepository.saveAndFlush(user(
                "plain@iter.test",
                "일반 회원",
                "plain",
                UserStatus.ACTIVE
        ));

        var percentResult = userRepository.searchForAdminByCursor(
                "%",
                null,
                null,
                null,
                PageRequest.of(0, 20)
        );
        var underscoreResult = userRepository.searchForAdminByCursor(
                "_",
                null,
                null,
                null,
                PageRequest.of(0, 20)
        );

        assertThat(percentResult)
                .extracting(User::getId)
                .containsExactly(percentMatch.getId());
        assertThat(underscoreResult)
                .extracting(User::getId)
                .containsExactly(underscoreMatch.getId());
    }

    @Test
    void 회원_상태_변경용_조회는_비관적_쓰기_락을_획득한다() {
        User saved = userRepository.saveAndFlush(user(
                "locked@iter.test",
                "락 대상 회원",
                "락대상",
                UserStatus.ACTIVE
        ));
        entityManager.clear();

        User found = userRepository.findWithLockById(saved.getId()).orElseThrow();

        assertThat(found.getId()).isEqualTo(saved.getId());
        assertThat(entityManager.getLockMode(found))
                .isEqualTo(LockModeType.PESSIMISTIC_WRITE);
    }

    private User user(
            String email,
            String name,
            String nickname,
            UserStatus status
    ) {
        return User.builder()
                .email(email)
                .password("encoded-password")
                .name(name)
                .nickname(nickname)
                .phone("010-0000-0000")
                .role(Role.USER)
                .status(status)
                .build();
    }
}
