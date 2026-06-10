package com.ecom.util;

import java.io.UnsupportedEncodingException;
import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import com.ecom.model.ProductOrder;
import com.ecom.model.UserDtls;
import com.ecom.service.UserService;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpServletRequest;

@Component
public class CommonUtil {

    // ==========================
    // MAIL CONFIG (TEMP DISABLED)
    // ==========================

    // @Autowired
    // private JavaMailSender mailSender;

    // @Value("${spring.mail.username}")
    // private String fromMail;

    // @Value("${spring.mail.host}")
    // private String host;

    // @Value("${spring.mail.port}")
    // private String port;

    @Autowired
    private UserService userService;

    // ==========================
    // PASSWORD RESET MAIL
    // ==========================

    /*
     * public Boolean sendMail(String url, String reciepentEmail)
     * throws UnsupportedEncodingException, MessagingException {
     * 
     * if (mailSender == null) {
     * System.out.println("Mail service disabled");
     * return true;
     * }
     * 
     * if (reciepentEmail == null || reciepentEmail.trim().isEmpty()) {
     * throw new
     * IllegalArgumentException("Recipient email is required for sendMail.");
     * }
     * 
     * MimeMessage message = mailSender.createMimeMessage();
     * MimeMessageHelper helper = new MimeMessageHelper(message);
     * 
     * helper.setFrom(fromMail, "Shopping cart");
     * helper.setTo(reciepentEmail);
     * 
     * String content = "<p>Hello,</p>"
     * + "<p>You have requested to reset your password.</p>"
     * + "<p>Click the link below to change your password:</p>"
     * + "<p><a href=\"" + url + "\">Change my password</a></p>";
     * 
     * helper.setSubject("Password Reset");
     * helper.setText(content, true);
     * 
     * mailSender.send(message);
     * 
     * return true;
     * }
     */

    public Boolean sendMail(String url, String reciepentEmail) {
        System.out.println("Password reset mail disabled");
        return true;
    }

    public static String generateUrl(HttpServletRequest request) {
        String siteUrl = request.getRequestURL().toString();
        return siteUrl.replace(request.getServletPath(), "");
    }

    // ==========================
    // ORDER MAIL
    // ==========================

    /*
     * public Boolean sendMailForProductOrder(ProductOrder order, String status)
     * throws Exception {
     * 
     * System.out.println("===== MAIL METHOD CALLED =====");
     * System.out.println("MAIL SENDER = " + mailSender);
     * System.out.println("FROM MAIL = " + fromMail);
     * 
     * if (mailSender == null) {
     * System.out.println("Mail service disabled");
     * return true;
     * }
     * 
     * String recipient = order.getOrderAddress().getEmail();
     * 
     * MimeMessage message = mailSender.createMimeMessage();
     * MimeMessageHelper helper = new MimeMessageHelper(message);
     * 
     * helper.setFrom(fromMail, "Shopping cart");
     * helper.setTo(recipient);
     * 
     * String subject = "Product Order Status";
     * helper.setSubject(subject);
     * 
     * helper.setText("Order Status : " + status, true);
     * 
     * mailSender.send(message);
     * 
     * return true;
     * }
     */

    public Boolean sendMailForProductOrder(ProductOrder order, String status) {
        System.out.println("Order mail disabled");
        return true;
    }

    private String safeStr(Object o) {
        return o == null ? "" : o.toString();
    }

    public UserDtls getLoggedInUserDetails(Principal p) {
        if (p == null) {
            return null;
        }

        String email = p.getName();
        return userService.getUserByEmail(email);
    }

}
