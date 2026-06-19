package com.example.teamflow.domain.member.service;

import com.example.teamflow.common.exception.BusinessException;
import com.example.teamflow.common.exception.ErrorCode;
import com.example.teamflow.domain.member.dto.LoginRequest;
import com.example.teamflow.domain.member.dto.LoginResponse;
import com.example.teamflow.domain.member.dto.RegisterRequest;
import com.example.teamflow.domain.member.entity.Member;
import com.example.teamflow.domain.member.repository.MemberRepository;
import com.example.teamflow.infra.security.JwtTokenProvider;
import com.example.teamflow.infra.security.TokenBlacklist;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklist tokenBlacklist;

    @Transactional
    public LoginResponse register(RegisterRequest req) {
        if (memberRepository.findByEmail(req.email()).isPresent()) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }
        Member member = Member.create(
                req.name(), req.role(), req.initial(),
                req.weeklyCapacityHours(), req.email(),
                passwordEncoder.encode(req.password()));
        List<String> skills = req.skills() != null ? req.skills() : List.of();
        skills.forEach(member::addSkill);
        memberRepository.save(member);
        return new LoginResponse(jwtTokenProvider.generateToken(member.getId(), member.getRole()));
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Member member = memberRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), member.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        String token = jwtTokenProvider.generateToken(member.getId(), member.getRole());
        return new LoginResponse(token);
    }

    public void logout(String token) {
        if (jwtTokenProvider.validateToken(token)) {
            tokenBlacklist.add(jwtTokenProvider.getJti(token), jwtTokenProvider.getExpiration(token));
        }
    }
}
