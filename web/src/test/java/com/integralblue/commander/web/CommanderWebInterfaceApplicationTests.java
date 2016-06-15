package com.integralblue.commander.web;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = CommanderWebInterfaceApplication.class)
@WebAppConfiguration
// Run as an integration test to workaround
// https://jira.spring.io/browse/SPR-14367
@IntegrationTest("server.port:0")
public class CommanderWebInterfaceApplicationTests {

	@Test
	public void contextLoads() {
	}

}
