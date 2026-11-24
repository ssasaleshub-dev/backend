package com.salescrm.service;

import com.salescrm.SalesCrmApp;
import com.salescrm.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = SalesCrmApp.class)
@ActiveProfiles("dev")
public class SendCleanPasswordResetRunnerTest {

    @Autowired
    private MailService mailService;

    @Test
    void sendCleanPasswordReset() throws Exception {
        User user = new User();
        user.setLogin("kavin");
        user.setFirstName("Kavin");
        user.setEmail("apkavin484@gmail.com");
        user.setLangKey("en");
        user.setResetKey("166116");

        mailService.sendPasswordResetMail(user);
        Thread.sleep(4000);
        System.out.println("✅ Clean human-designed verification code email dispatched to apkavin484@gmail.com!");
    }
}
