package com.study.study_community_platform.controller;

import com.study.study_community_platform.controller.web.argumentresolver.Login;
import com.study.study_community_platform.domain.Member;
import com.study.study_community_platform.service.CommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
@Controller
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    // 댓글 작성
    @PostMapping("/studies/{studyId}/comments")
    public String addComment(@Login Member loginMember,
                             @PathVariable Long studyId,
                             @RequestParam String content){

        // 등록 후 스터디 상세 페이지로 이동
        commentService.registerComment(loginMember.getId(), studyId, content);
        return "redirect:/studies/" + studyId;
    }

    // 댓글 수정
    @PostMapping("/studies/{studyId}/comments/{commentId}/edit")
    public String editComment(@Login Member loginMember,
                              @PathVariable Long studyId,
                              @PathVariable Long commentId,
                              @RequestParam String content){

        // 내가 쓴 댓글 일때만 수정
        try{
            commentService.updateComment(loginMember.getId(), commentId, content);
        }catch(IllegalStateException e){
            log.warn("권한 없는 사용자의 댓글 수정 시도. memberId={}, commentId={}", loginMember.getId(), commentId);
        }

        return "redirect:/studies/" + studyId;
    }

    // 댓글 삭제
    @PostMapping("/studies/{studyId}/comments/{commentId}/delete")
    public String deleteComment(@Login Member loginMember,
                                @PathVariable Long studyId,
                                @PathVariable Long commentId){

        // 내가 쓴 댓글 일때만 삭제
        try{
            commentService.deleteComment(loginMember.getId(), commentId);
        }catch(IllegalStateException e){
            log.warn("권한 없는 사용자의 댓글 삭제 시도. memberId={}, commentId={}", loginMember.getId(), commentId);
        }

        return "redirect:/studies/"+studyId;
    }


}
