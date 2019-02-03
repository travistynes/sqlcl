package p;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

@Component
public class CLR implements CommandLineRunner {
	private static final Logger log = LoggerFactory.getLogger(CLR.class);

	@Autowired
	private SqlExecutor sqlExecutor;

	@Override
	public void run(String... args) throws Exception {
		sqlExecutor.execute();
	}
}
