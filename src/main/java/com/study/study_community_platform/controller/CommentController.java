package com.study.study_community_platform.controller;

import com.study.study_community_platform.controller.web.argumentresolver.Login;
import com.study.study_community_platform.controller.web.comment.CommentForm;
import com.study.study_community_platform.controller.web.session.LoginMemberSession;
import com.study.study_community_platform.service.CommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    // 댓글 작성
    @PostMapping("/studies/{studyId}/comments")
    public String addComment(@Login LoginMemberSession loginMember,
                             @PathVariable Long studyId,
                             @Validated @ModelAttribute("commentForm") CommentForm form,
                             BindingResult bindingResult,
                             RedirectAttributes redirectAttributes){

        // 검증 실패 시
        if(bindingResult.hasErrors()){
            // 상세 화면에 표시할 첫 번째 오류 메시지 전달
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    bindingResult.getAllErrors().get(0).getDefaultMessage()
            );

            // 사용자가 작성한 내용을 다시 입력란에 보여주기 위해 보관
            redirectAttributes.addFlashAttribute(
                    "commentDraft",
                    form.getContent()
            );

            return "redirect:/studies/" + studyId;
        }

        // 등록 성공하면 메시지 저장하고 상세 페이지로 이동
        commentService.registerComment(loginMember.id(), studyId, form.getContent());
        redirectAttributes.addFlashAttribute(
                "successMessage",
                "댓글이 등록되었습니다."
        );

        return "redirect:/studies/" + studyId;
    }

    // 댓글 수정
    @PostMapping("/studies/{studyId}/comments/{commentId}/edit")
    public String editComment(@Login LoginMemberSession loginMember,
                              @PathVariable Long studyId,
                              @PathVariable Long commentId,
                              @Validated @ModelAttribute("commentForm") CommentForm form,
                              BindingResult bindingResult,
                              RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    bindingResult.getAllErrors().get(0).getDefaultMessage()

            );

            // 여러 댓글 중 어떤 댓글의 수정이 실패했는지 전달
            redirectAttributes.addFlashAttribute(
                    "editingCommentId",
                    commentId
            );

            // 해당 댓글의 수정 입력란에 복원할 내용 전달
            redirectAttributes.addFlashAttribute(
                    "editingCommentDraft",
                    form.getContent()
            );

            return "redirect:/studies/" + studyId;
        }

        commentService.updateComment(
                loginMember.id(),
                commentId,
                form.getContent()
        );

        redirectAttributes.addFlashAttribute(
                "successMessage",
                "댓글이 수정되었습니다."
        );

        return "redirect:/studies/" + studyId;

    }

    // 댓글 삭제
    @PostMapping("/studies/{studyId}/comments/{commentId}/delete")
    public String deleteComment(@Login LoginMemberSession loginMember,
                                @PathVariable Long studyId,
                                @PathVariable Long commentId){

        // 내가 쓴 댓글 일때만 삭제
        commentService.deleteComment(loginMember.id(), commentId);

        return "redirect:/studies/"+studyId;
    }


}
