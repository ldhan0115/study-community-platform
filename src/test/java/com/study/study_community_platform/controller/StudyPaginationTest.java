package com.study.study_community_platform.controller;

import com.study.study_community_platform.domain.Member;
import com.study.study_community_platform.domain.Study;
import com.study.study_community_platform.domain.StudyMethod;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Page;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:study-pagination-test;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class StudyPaginationTest {

    private static final LocalDateTime SAME_TIME =
            LocalDateTime.of(2026, 1, 1, 0, 0);

    @Autowired
    MockMvc mockMvc;

    @Autowired
    EntityManager entityManager;

    @Test
    void guestGetsFirstTenStudiesByDefault() throws Exception {
        createStudies(12);

        MvcResult result = mockMvc.perform(get("/studies"))
                .andExpect(status().isOk())
                .andExpect(view().name("studies/studyList"))
                .andReturn();

        Page<Study> page = pageOf(result);

        assertThat(page.getNumber()).isZero();
        assertThat(page.getSize()).isEqualTo(10);
        assertThat(page.getNumberOfElements()).isEqualTo(10);
        assertThat(page.getTotalElements()).isEqualTo(12L);
    }

    @Test
    void pagesKeepOrderAndExcludeDeletedStudies() throws Exception {
        List<Long> ids = createStudies(26);

        Long deletedId = ids.get(10);
        Long newestId = ids.get(0);

        entityManager.find(Study.class, deletedId).withdraw();
        entityManager.flush();

        // 가장 작은 ID를 가장 최근 글로 만들어 createdAt 우선 정렬도 검증
        entityManager.createQuery(
                        "update Study s set s.createdAt = :time where s.id = :id"
                )
                .setParameter("time", SAME_TIME.plusDays(1))
                .setParameter("id", newestId)
                .executeUpdate();

        entityManager.clear();

        List<Long> expected = new ArrayList<>(ids);
        expected.remove(deletedId);
        expected.sort(Comparator.reverseOrder());

        // 최근 생성 시각이 우선이고, 같은 시각에서는 ID 내림차순
        expected.remove(newestId);
        expected.add(0, newestId);

        List<Long> actual = new ArrayList<>();

        for (int number = 0; number < 3; number++) {
            Page<Study> page = pageOf(requestPage(number, 10));

            assertThat(page.getTotalElements()).isEqualTo(25L);
            assertThat(page.getTotalPages()).isEqualTo(3);

            assertThat(page.getNumberOfElements())
                    .isEqualTo(number == 2 ? 5 : 10);

            actual.addAll(
                    page.getContent().stream()
                            .map(Study::getId)
                            .toList()
            );

            entityManager.clear();
        }

        // 25개 전체의 순서·중복·누락과 삭제 제외를 함께 검증
        assertThat(actual)
                .containsExactlyElementsOf(expected)
                .doesNotHaveDuplicates();

        assertThat(actual).doesNotContain(deletedId);
    }

    @ParameterizedTest
    @CsvSource({
            "-1, 10, 0, 10",
            "0, 0, 0, 10",
            "0, -5, 0, 10",
            "0, 1, 0, 1",
            "0, 50, 0, 50",
            "0, 1000, 0, 50"
    })
    void normalizesPageAndSize(
            int page,
            int size,
            int expectedPage,
            int expectedSize
    ) throws Exception {
        createStudies(55);

        Page<Study> result = pageOf(requestPage(page, size));

        assertThat(result.getNumber()).isEqualTo(expectedPage);
        assertThat(result.getSize()).isEqualTo(expectedSize);
        assertThat(result.getNumberOfElements()).isEqualTo(expectedSize);
        assertThat(result.getTotalElements()).isEqualTo(55L);
    }

    @Test
    void navigationPreservesSizeAndDisablesBoundaryLinks() throws Exception {
        createStudies(12);

        String first = html(requestPage(0, 5));

        assertThat(first).doesNotContain("id=\"study-page-prev\"");
        assertThat(first).contains("id=\"study-page-next\"");

        String middle = html(requestPage(1, 5));

        assertThat(middle).contains("/studies?page=0&amp;size=5");
        assertThat(middle).contains("/studies?page=2&amp;size=5");

        MvcResult last = requestPage(2, 5);

        assertThat(pageOf(last).getNumberOfElements()).isEqualTo(2);
        assertThat(pageOf(last).hasNext()).isFalse();

        assertThat(html(last)).contains("id=\"study-page-prev\"");
        assertThat(html(last)).doesNotContain("id=\"study-page-next\"");
    }

    @Test
    void emptyDatabaseShowsEmptyMessage() throws Exception {
        MvcResult result = requestPage(0, 10);

        assertThat(pageOf(result).getTotalElements()).isZero();
        assertThat(pageOf(result).getContent()).isEmpty();

        assertThat(html(result)).contains("등록된 스터디가 없습니다.");
        assertThat(html(result)).doesNotContain("id=\"study-page-next\"");
    }

    @Test
    void outOfRangePageOffersFirstPageLink() throws Exception {
        createStudies(1);

        MvcResult result = requestPage(99, 10);

        assertThat(pageOf(result).getContent()).isEmpty();
        assertThat(pageOf(result).getTotalElements()).isEqualTo(1L);

        assertThat(html(result))
                .contains("요청한 페이지에 스터디가 없습니다.");

        assertThat(html(result))
                .contains("/studies?page=0&amp;size=10");
    }

    @ParameterizedTest
    @CsvSource({
            "page, abc",
            "size, abc",
            "page, 2147483647"
    })
    void invalidNumberOrOffsetReturns400(
            String parameter,
            String value
    ) throws Exception {
        mockMvc.perform(
                        get("/studies").param(parameter, value)
                )
                .andExpect(status().isBadRequest());
    }

    private MvcResult requestPage(int page, int size) throws Exception {
        return mockMvc.perform(
                        get("/studies")
                                .param("page", String.valueOf(page))
                                .param("size", String.valueOf(size))
                )
                .andExpect(status().isOk())
                .andExpect(view().name("studies/studyList"))
                .andReturn();
    }

    @SuppressWarnings("unchecked")
    private Page<Study> pageOf(MvcResult result) {
        return (Page<Study>) result
                .getModelAndView()
                .getModel()
                .get("studyPage");
    }

    private String html(MvcResult result) throws Exception {
        return result.getResponse()
                .getContentAsString(StandardCharsets.UTF_8);
    }

    private List<Long> createStudies(int count) {
        // 로그인 테스트가 아니므로 비밀번호는 테스트용 저장값만 사용
        Member owner = Member.createMember(
                "pageOwner",
                "test-encoded-password",
                "page@test.com",
                "페이지작성자"
        );

        entityManager.persist(owner);

        List<Long> ids = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            Study study = Study.createStudy(
                    owner,
                    "페이지 스터디 " + i,
                    "본문",
                    StudyMethod.ONLINE,
                    null,
                    5
            );

            entityManager.persist(study);
            ids.add(study.getId());
        }

        entityManager.flush();

        // 생성 시각이 같은 데이터로 ID 보조 정렬을 검증
        entityManager.createQuery(
                        "update Study s set s.createdAt = :time"
                )
                .setParameter("time", SAME_TIME)
                .executeUpdate();

        entityManager.clear();

        return ids;
    }
}