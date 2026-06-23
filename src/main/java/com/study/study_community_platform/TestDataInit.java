package com.study.study_community_platform;

import com.study.study_community_platform.domain.Comment;
import com.study.study_community_platform.domain.Member;
import com.study.study_community_platform.domain.Study;
import com.study.study_community_platform.domain.StudyMethod;
import com.study.study_community_platform.repository.CommentRepository;
import com.study.study_community_platform.repository.MemberRepository;
import com.study.study_community_platform.repository.StudyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class TestDataInit {

    private final MemberRepository memberRepository;
    private final StudyRepository studyRepository;
    private final CommentRepository commentRepository;

    @EventListener(ApplicationReadyEvent.class) // 컨테이너 준비 완료 후 실행
    @Transactional // 트랜잭션 보장
    public void init(){

        Member member1 = Member.createMember("test1", "test1234", "test1@example.com", "대한이");
        memberRepository.save(member1);

        Member member2 = Member.createMember("test2", "test5678", "test2@example.com", "지혜");
        memberRepository.save(member2);

        Study study1 = Study.createStudy(member1, "취뽀 기원", "취업 할거예용", StudyMethod.OFFLINE, "수원", 4);
        studyRepository.save(study1);

        Study study2 = Study.createStudy(member2, "철 밥통 개꿀", "공무원 될거예용", StudyMethod.OFFLINE, "세종", 4);
        studyRepository.save(study2);

        Comment comment1 = Comment.createComment(member1, study2, "저도 참가합니다!");
        commentRepository.save(comment1);

        Comment comment2 = Comment.createComment(member2, study1, "저도 같이 힘낼게요!");
        commentRepository.save(comment2);

    }
}


