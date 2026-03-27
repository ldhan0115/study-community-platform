package com.study.study_community_platform.controller;

import com.study.study_community_platform.domain.Member;
import com.study.study_community_platform.service.MemberService;
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

        Member member = new Member(form.getLoginId(), form.getPassword(), form.getEmail(), form.getNickname());
        memberService.join(member);
        return "redirect:/members/login";
    }
}
