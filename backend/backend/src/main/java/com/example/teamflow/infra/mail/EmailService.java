package com.example.teamflow.infra.mail;

import jakarta.mail.MessagingException;
import java.io.UnsupportedEncodingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Slf4j
@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String from;

    @Value("${app.base-url:http://localhost:3000}")
    private String baseUrl;

    public EmailService(@Autowired(required = false) JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /** 이미 가입된 사용자에게 앱 내 참가 요청 확인 알림 발송 */
    @Async
    public void sendInviteNotification(String to, String inviterName, String workspaceName) {
        if (!isConfigured()) return;
        String content =
                "<p>" + inviterName + "님이 <strong>" + workspaceName + "</strong> 워크스페이스에 초대했습니다.</p>" +
                "<p style=\"margin-bottom:24px;\">TeamFlow에 로그인한 뒤 <strong>팀</strong> 화면에서 참가 요청을 수락하세요.</p>" +
                "<a href=\"" + baseUrl + "\" " +
                "style=\"display:inline-block;padding:11px 28px;background:#6366f1;color:#fff;" +
                "border-radius:8px;text-decoration:none;font-weight:600;font-size:14px;\">TeamFlow 열기</a>";
        send(to, "[TeamFlow] " + inviterName + "님이 " + workspaceName + " 워크스페이스에 초대했습니다", buildHtml(content), inviterName);
    }

    /** 아직 가입하지 않은 이메일에 가입 링크 포함 초대 발송 */
    @Async
    public void sendSignupInvite(String to, String inviterName, String workspaceName, String inviteToken) {
        if (!isConfigured()) return;
        String signupLink = baseUrl + "/signup?token=" + inviteToken;
        String content =
                "<p>" + inviterName + "님이 <strong>" + workspaceName + "</strong> 워크스페이스에 초대했습니다.</p>" +
                "<p style=\"margin-bottom:24px;\">아래 버튼을 눌러 가입하면 <strong>" + workspaceName + "</strong>에 자동으로 합류됩니다.</p>" +
                "<a href=\"" + signupLink + "\" " +
                "style=\"display:inline-block;padding:11px 28px;background:#6366f1;color:#fff;" +
                "border-radius:8px;text-decoration:none;font-weight:600;font-size:14px;\">가입하고 합류하기</a>" +
                "<p style=\"margin-top:20px;font-size:12px;color:#9ca3af;\">이 링크는 7일 후 만료됩니다.</p>";
        send(to, "[TeamFlow] " + inviterName + "님이 " + workspaceName + " 워크스페이스에 초대했습니다", buildHtml(content), inviterName);
    }

    private boolean isConfigured() {
        if (mailSender == null || !StringUtils.hasText(from)) {
            log.debug("이메일 미설정 — 발송 생략 (MAIL_USERNAME 환경변수를 설정하세요)");
            return false;
        }
        return true;
    }

    private void send(String to, String subject, String html) {
        send(to, subject, html, null);
    }

    private void send(String to, String subject, String html, String senderName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            try {
                String displayName = senderName != null ? senderName + " (via TeamFlow)" : "TeamFlow";
                helper.setFrom(from, displayName);
            } catch (UnsupportedEncodingException e) {
                helper.setFrom(from);
            }
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(message);
            log.info("이메일 발송 완료: to={}", to);
        } catch (MessagingException e) {
            log.warn("이메일 발송 실패: to={}, error={}", to, e.getMessage());
        }
    }

    private String buildHtml(String content) {
        return """
                <!DOCTYPE html>
                <html lang="ko">
                <head><meta charset="UTF-8"><meta name="viewport" content="width=device-width,initial-scale=1"></head>
                <body style="margin:0;padding:0;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;background:#f3f4f6;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="padding:48px 20px;">
                    <tr><td align="center">
                      <table width="480" cellpadding="0" cellspacing="0"
                             style="background:#ffffff;border-radius:12px;overflow:hidden;box-shadow:0 1px 4px rgba(0,0,0,0.08);">
                        <tr>
                          <td style="background:#6366f1;padding:22px 32px;">
                            <span style="color:#fff;font-size:20px;font-weight:700;letter-spacing:-0.3px;">TeamFlow</span>
                          </td>
                        </tr>
                        <tr>
                          <td style="padding:32px;font-size:15px;color:#111827;line-height:1.6;">
                            %s
                          </td>
                        </tr>
                        <tr>
                          <td style="background:#f9fafb;padding:16px 32px;font-size:12px;color:#9ca3af;border-top:1px solid #f3f4f6;">
                            이 이메일은 TeamFlow에서 자동으로 발송되었습니다. 문의 사항은 관리자에게 연락하세요.
                          </td>
                        </tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(content);
    }
}
