import com.shika.textMessageApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.Map;

import static java.lang.Thread.sleep;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = textMessageApplication.class)
public class TmTest {
    @Autowired
    private AmqpTemplate template;

    @Test
    public void testSendText() throws InterruptedException {
        Map<String, String> msg = new HashMap<>();
        msg.put("phone", "13366567815");
        msg.put("code", "15672");
        template.convertAndSend("shika.tm.exchange", "tm.verify.code", msg);
        sleep(10000L);
    }
}
