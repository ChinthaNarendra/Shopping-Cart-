package com.ecom.util;

import java.io.UnsupportedEncodingException;
import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.ecom.model.ProductOrder;
import com.ecom.model.UserDtls;
import com.ecom.service.UserService;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.servlet.http.HttpServletRequest;

@Component
public class CommonUtil {

    @Autowired
    private JavaMailSender mailSender;
    
    @Autowired
    private UserService userService;
    

    // process to send the mail (password reset)
    public Boolean sendMail(String url, String reciepentEmail) throws UnsupportedEncodingException, MessagingException {

        if (reciepentEmail == null || reciepentEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("Recipient email is required for sendMail.");
        }

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message);
        helper.setFrom("chinthanarendracn@gmail.com", "Shopping cart");
        helper.setTo(reciepentEmail);

        String content = "<p>Hello,</p>"
                + "<p>You have requested to reset your password.</p>"
                + "<p>Click the link below to change your password:</p>"
                + "<p><a href=\"" + url + "\">Change my password</a></p>";

        helper.setSubject("Password Reset");
        helper.setText(content, true);
        mailSender.send(message);
        return true;
    }

    public static String generateUrl(HttpServletRequest request) {
        String siteUrl = request.getRequestURL().toString();
        return siteUrl.replace(request.getServletPath(), "");
    }

    /**
     * Sends product order status mail.
     *
     * NOTE: consider annotating this with @Async to avoid blocking the request thread:
     *   @Async
     *   public Boolean sendMailForProductOrder(...) { ... }
     * Also enable async in configuration with @EnableAsync.
     */
    public Boolean sendMailForProductOrder(ProductOrder order, String status) throws Exception {

        // Defensive checks
        if (order == null) {
            throw new IllegalArgumentException("Order is required.");
        }
        if (order.getOrderAddress() == null) {
            throw new IllegalArgumentException("Order address is required.");
        }
        String recipient = order.getOrderAddress().getEmail();
        if (recipient == null || recipient.trim().isEmpty()) {
            throw new IllegalArgumentException("Recipient email is required in order address.");
        }

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message);
        helper.setFrom("chinthanarendracn@gmail.com", "Shopping cart");
        helper.setTo(recipient);

        // local template (won't mutate any class-level state)
        String template = "<p>[[name]]</p>"
                + "<p>Thank You for ordering...!! Your Order is <b>[[orderStatus]]</b>.</p>"
                + "<p><b>Product Details :</b> </p>"
                + "<p>Name : [[productName]]</p>"
                + "<p>Category : [[category]]</p>"
                + "<p>Quantity : [[quantity]]</p>"
                + "<p>Price : [[price]]</p>"
                + "<p>Payment Type : [[paymentType]]</p>";

        // category text extraction
        String categoryText = "";
        try {
            if (order.getProduct() != null && order.getProduct().getCategory() != null) {
                Object catObj = order.getProduct().getCategory();
                if (catObj instanceof com.ecom.model.Category) {
                    categoryText = ((com.ecom.model.Category) catObj).getName();
                } else {
                    categoryText = String.valueOf(catObj);
                }
            }
        } catch (Exception e) {
            categoryText = "";
        }

        String msg = template.replace("[[name]]", safeStr(order.getOrderAddress().getFirstName()))
                .replace("[[orderStatus]]", status == null ? "" : status)
                .replace("[[productName]]", order.getProduct() == null ? "" : safeStr(order.getProduct().getTitle()))
                .replace("[[category]]", safeStr(categoryText))
                .replace("[[quantity]]", order.getQuantity() == null ? "0" : order.getQuantity().toString())
                .replace("[[price]]", order.getPrice() == null ? "0.0" : order.getPrice().toString())
                .replace("[[paymentType]]", safeStr(order.getPaymentType()));

        // subject includes order id/date for traceability
        String subject = "Product Order Status";
        if (order.getOrderId() != null) {
            subject += " - Order: " + order.getOrderId();
        }
        helper.setSubject(subject);
        helper.setText(msg, true); // true => HTML
        mailSender.send(message);
        return true;
    }

    // helper to avoid NPE on string fields
    private String safeStr(Object o) {
        return o == null ? "" : o.toString();
    }
    
    public UserDtls getLoggedInUserDetails(Principal p) {
		if (p == null) {
			return null;
		}
		String email = p.getName();
		UserDtls userDtls = userService.getUserByEmail(email);
		return userDtls;
	}
    
   
}
