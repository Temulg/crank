/*
 * Copyright (c) 2018 Alex Dubov <oakad@yahoo.com>
 *
 * This file is made available under the GNU General Public License
 * version 3 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.gnu.org/licenses/gpl-3.0.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package udentric.crank;

import java.util.Collections;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.AppenderComponentBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilder;
import org.apache.logging.log4j.core.config.builder.api.ConfigurationBuilderFactory;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.logging.log4j.message.Message;

public class TestApp {
	static class A {}
	static class B {}
	static class C {}

	static class D {
		public D(A a, C c) {}
	}

	static class E {
		public E(D d, B b) {}
	}

	static class F {
		public F(D d, C c) {}
	}

	static class G {
		public G(F f, E e, H h) {}
	}

	static class H {
	}

	public static void main(String... args) throws Exception {
		Logger logger = configLog();
		Message m = logger.traceEntry("before crank");

		ActivationSet as = Crank.with(
			new A(), new B(), new C()
		).withSuppliers(new Object() {
			@CrankSupplier
			public H make(B b) {
				return new H();
			}
		}).withLoggerContext(
			Collections.singletonMap("aaab", "snug")
		).start(F.class, G.class);

		logger.info("crank started");

		as.close();

		logger.traceExit("after crank");
	}

	private static Logger configLog() {
		ConfigurationBuilder<
			BuiltConfiguration
		> builder = ConfigurationBuilderFactory.newConfigurationBuilder();
		builder.setStatusLevel(Level.ERROR);
		builder.setConfigurationName("TestApp");
		AppenderComponentBuilder appenderBuilder = builder.newAppender(
			"Stdout", "CONSOLE"
		).addAttribute("target", ConsoleAppender.Target.SYSTEM_OUT);
		builder.add(appenderBuilder);
		builder.add(builder.newRootLogger(Level.ALL).add(
			builder.newAppenderRef("Stdout"))
		);
		LoggerContext ctx = Configurator.initialize(builder.build());
		return ctx.getLogger("TestApp");
	}
}
