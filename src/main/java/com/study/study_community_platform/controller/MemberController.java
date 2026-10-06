package com.study.study_community_platform.controller;

import com.study.study_community_platform.controller.web.member.EditMemberForm;
import com.study.study_community_platform.controller.web.member.JoinMemberForm;
import com.study.study_community_platform.controller.web.member.LoginMemberForm;
import com.study.study_community_platform.controller.web.SessionConst;
import com.study.study_community_platform.controller.web.argumentresolver.Login;
import com.study.study_community_platform.controller.web.session.LoginMemberSession;
import com.study.study_community_platform.domain.Member;
import com.study.study_community_platform.exception.DuplicateMemberException;
import com.study.study_community_platform.service.MemberService;
import com.study.study_community_platform.service.dto.MemberUpdateDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/members")
public class MemberController {

    private final MemberService memberService;

    // 회원가입 폼 이동
    @GetMapping("/join")
    public String joinForm(Model model){
        model.addAttribute("member", new JoinMemberForm());
        return "members/joinMemberForm";
    }

    // 회원가입
    @PostMapping("/join")
    public String join(@Validated @ModelAttribute("member") JoinMemberForm form, BindingResult bindingResult) {

        // 필드 검증 실패 시 회원가입 화면 다시 이동
        if (bindingResult.hasErrors()) {
            return "members/joinMemberForm";
        }

        // 중복 검사 예외 처리
        try {
            memberService.join(form);
        } catch (DuplicateMemberException e) {
            bindingResult.rejectValue(
                    e.getField(),
                    "duplicate",
                    e.getMessage()
            );

            return "members/joinMemberForm";
        } catch (DataIntegrityViolationException e) {
            // 사전 검사 이후 DB에서 발생한 중복 충돌을 처리
            rejectUniqueConflict(e, bindingResult);
            return "members/joinMemberForm";
        }

        return "redirect:/members/login";
    }

    // 로그인 폼 이동
    @GetMapping("/login")
    public String loginForm(Model model){
        model.addAttribute("member", new LoginMemberForm());
        return "members/loginMemberForm";
    }

    // 회원 정보 수정 폼으로 이동
    @GetMapping("/edit")
    // @Login 애노테이션 활용
    // 기존 로그인된 Member 객체를 받는 것에서 필요한 데이터만 저장해놓은 DTO를 받아 활용하는 것으로 수정
    public String editForm(@Login LoginMemberSession loginMember, Model model){

        EditMemberForm form = new EditMemberForm();

        // 세션에는 최소 정보만 존재하므로 회원 수정 화면에 필요한 데이터는 DB에서 최신 상태로 다시 조회해서 대입
        Member member = memberService.findMember(loginMember.id());
        form.setLoginId(member.getLoginId());
        form.setEmail(member.getEmail());
        form.setNickname(member.getNickname());

        model.addAttribute("member", form);
        return "members/editMemberForm";
    }

    // 회원정보 수정
    // 대상 회원 ID를 브라우저가 보내는 값이 아닌 서버의 로그인 정보에서 가져오는 것으로 수정
    @PostMapping("/edit")
    public String edit(@Validated @ModelAttribute("member") EditMemberForm form,
                       BindingResult bindingResult,
                       @Login LoginMemberSession loginMember,
                       HttpServletRequest request){

        // 필드 검증 실패 시 수정 화면 다시 이동
        if(bindingResult.hasErrors()) {
            return "members/editMemberForm";
        }

        // 사용자의 입력을 받은 form을 직접 전달 x
        // 회원 수정에 필요한 데이터 DTO로 변환해서 전달
        MemberUpdateDto memberUpdateDto =
                new MemberUpdateDto(
                    form.getLoginId(), form.getPassword(),
                    form.getEmail(), form.getNickname()
                );

        Member updatedMember;

        try{
            updatedMember = memberService.editMember(loginMember.id(), memberUpdateDto);
        }catch (DuplicateMemberException e){
            bindingResult.rejectValue(
                    e.getField(),
                    "duplicate",
                    e.getMessage()
            );

            // 세션 갱신 코드에 도달하지 않으므로 기존 세션 유지
            return "members/editMemberForm";

        }catch(DataIntegrityViolationException e){
            rejectUniqueConflict(e, bindingResult);
            return "members/editMemberForm";
        }

        // 정보 수정 완료 후 변경된 정보가 화면에 반영되도록 세션 정보 갱신
        HttpSession session = request.getSession();
        if(session == null){
            throw new IllegalStateException("로그인 세션이 존재하지 않습니다.");
        }


        // 기존 비영속 객체를 세션에 저장한 것에서 DB에서 조회하여 수정한 영속 회원을 저장하여 id가 null이 되는 문제 해결
        session.setAttribute(SessionConst.LOGIN_MEMBER, LoginMemberSession.from(updatedMember));
        return "redirect:/";

    }

    // 회원 탈퇴
    @PostMapping("/withdraw")
    public String withdraw(@Login LoginMemberSession loginMember,
                           HttpServletRequest request,
                           HttpServletResponse response){

        memberService.withdrawMember(loginMember.id());

        // 탈퇴를 수행한 현재 요청의 인증 정보와 세션을 함께 정리
        new SecurityContextLogoutHandler().logout(
                request,
                response,
                SecurityContextHolder.getContext().getAuthentication()
        );

        return "redirect:/";
    }

    private void rejectUniqueConflict(DataIntegrityViolationException exception, BindingResult bindingResult){
        Throwable cause = exception;

        // Spring이 감싼 예외 안에서 실제 Hibernate 제약 위반을 찾음
        while(cause != null){
            if(cause instanceof ConstraintViolationException violation &&
                    violation.getKind() == ConstraintViolationException.ConstraintKind.UNIQUE){

                // DB 충돌에서는 필드를 추측하지 않고 폼 전체 오류로 안내
                bindingResult.reject(
                        "duplicate.concurrent",
                        "이미 사용 중인 회원 정보가 있습니다. 아이디, 이메일, 닉네임을 확인해주세요."
                );
                return;
            }
            cause = cause.getCause();
        }
        // NOT NULL, 외래키 등 다른 DB 오류를 중복으로 잘못 안내하지 않음
        throw exception;
    }


}
