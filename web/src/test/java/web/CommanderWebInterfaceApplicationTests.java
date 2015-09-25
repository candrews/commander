package web;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.web.WebAppConfiguration;

import com.integralblue.commander.web.CommanderWebInterfaceApplication;

import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = CommanderWebInterfaceApplication.class)
@WebAppConfiguration
public class CommanderWebInterfaceApplicationTests {

	@Test
	public void contextLoads() {
	}

}
