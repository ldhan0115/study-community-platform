package com.study.study_community_platform.service;

import com.study.study_community_platform.domain.Comment;
import com.study.study_community_platform.domain.Member;
import com.study.study_community_platform.domain.Study;
import com.study.study_community_platform.exception.ForbiddenOperationException;
import com.study.study_community_platform.exception.ResourceNotFoundException;
import com.study.study_community_platform.repository.CommentRepository;
import com.study.study_community_platform.repository.StudyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final StudyRepository studyRepository;
    private final ActiveMemberService activeMemberService;

    // 댓글 등록
    @Transactional
    public Long registerComment(Long memberId, Long studyId, String content) {

        // 댓글 작성 회원 존재 여부 확인
        Member member = activeMemberService.requireActive(memberId);

        // 댓글 작성 스터디 존재 여부 확인
        Study study = studyRepository.findByIdAndDeletedAtIsNull(studyId)
                .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 스터디입니다."));

        // Comment 객체 생성 메서드를 사용
        Comment comment = Comment.createComment(member, study, content);

        commentRepository.save(comment);
        return comment.getId();
    }

    // 댓글 단건 조회
    public Comment findComment(Long commentId) {
        return commentRepository.findActiveById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("존재하지 않는 댓글입니다."));
    }

    // 특정 회원의 댓글 목록 조회
    public List<Comment> findCommentsByMember(Long memberId) {
        return commentRepository.findByMemberId(memberId);
    }

    // 특정 스터디의 댓글 목록 조회
    public List<Comment> findCommentsByStudyId(Long studyId) {
        return commentRepository.findByStudyId(studyId);
    }

    // 전체 댓글 조회
    public List<Comment> findComments() {
        return commentRepository.findAll();
    }

    // 스터디에 소속하는 댓글 조회
    private Comment findCommentInStudy(Long studyId, Long commentId){
        Comment comment = findComment(commentId);

        if(!comment.getStudy().getId().equals(studyId)){
            throw new ResourceNotFoundException("해당 스터디의 댓글을 찾을 수 없습니다.");
        }

        return comment;
    }

    // 댓글 수정
    @Transactional
    public void updateComment(Long memberId, Long studyId, Long commentId, String content) {

        activeMemberService.requireActive(memberId);

        // 활성 부모와 실제 소속 확인하면서 조회
        Comment comment = findCommentInStudy(studyId, commentId);

        if(!comment.getMember().getId().equals(memberId)){
            throw new ForbiddenOperationException("댓글 수정 권한이 없습니다.");
        }

        comment.changeCommentInfo(content);
    }

    // 댓글 삭제
    @Transactional
    public void deleteComment(Long memberId, Long studyId, Long commentId) {

        activeMemberService.requireActive(memberId);

        Comment comment = findCommentInStudy(studyId, commentId);

        if(!comment.getMember().getId().equals(memberId)){
            throw new ForbiddenOperationException("댓글 삭제 권한이 없습니다.");
        }

        comment.withdraw();
    }


}
