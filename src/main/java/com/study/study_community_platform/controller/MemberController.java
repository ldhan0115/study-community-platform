package com.study.study_community_platform.controller;

import com.study.study_community_platform.controller.web.JoinMemberForm;
import com.study.study_community_platform.controller.web.LoginMemberForm;
import com.study.study_community_platform.controller.web.SessionConst;
import com.study.study_community_platform.domain.Member;
import com.study.study_community_platform.service.MemberService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

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
    public String join(@Validated @ModelAttribute("member") JoinMemberForm form, BindingResult bindingResult){

        // 필드 검증 실패 시 회원가입 화면 다시 이동
        if(bindingResult.hasErrors()){
            return "members/joinMemberForm";
        }

        Member member = Member.createMember(form.getLoginId(), form.getPassword(), form.getEmail(), form.getNickname());
        memberService.join(member);
        return "redirect:/members/login";
    }

    // 로그인 폼 이동
    @GetMapping("/login")
    public String loginForm(Model model){
        model.addAttribute("member", new LoginMemberForm());
        return "members/loginMemberForm";
    }

    // 로그인
    @PostMapping("/login")
    public String login(@Validated @ModelAttribute("member") LoginMemberForm form,
                        BindingResult bindingResult,
                        HttpServletRequest request){

        // 필드 검증 실패 시 로그인 화면으로
        if(bindingResult.hasErrors()){
            return "members/loginMemberForm";
        }

        // 아이디/비밀번호 일치하는 회원 반환
        Member loginMember = memberService.login(form.getLoginId(), form.getPassword());

        // 인증 실패 시 글로벌 에러 담아서 로그인 화면으로 이동
        if(loginMember == null){
            bindingResult.reject("loginFail", "아이디 또는 비밀번호가 일치하지 않습니다.");
            return "members/loginMemberForm";
        }

        // 인증 성공 시 세션 생성하고 회원 정보 저장
        HttpSession session = request.getSession();
        session.setAttribute(SessionConst.LOGIN_MEMBER, loginMember);
        return "redirect:/";

    }

    @PostMapping("/logout")
    public String logout(HttpServletRequest request){

        // 기존 세션만 가져오고 새로 생성하지 않음
        HttpSession session = request.getSession(false);
        if(session != null){
            // 세션 정보 완전 삭제 및 무효화
            session.invalidate();
        }

        return "redirect:/";
    }
}
