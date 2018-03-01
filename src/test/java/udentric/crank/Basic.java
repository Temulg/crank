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

import java.util.ArrayDeque;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import org.testng.Assert;
import org.testng.annotations.Test;

public class Basic {
	static class A {
		public void start() {
			Assert.assertEquals(START_SEQ.size(), 0);
			START_SEQ.offerLast("A");
		}

		public void stop() {
			Assert.assertEquals(START_SEQ.pollLast(), "A");
		}
	}

	static class B {
		public void start() {
			Assert.assertEquals(START_SEQ.size(), 4);
			Assert.assertEquals(START_SEQ.peekLast(), "F");
			START_SEQ.offerLast("B");
		}

		public void stop() {
			Assert.assertEquals(START_SEQ.pollLast(), "B");
		}
	}

	static class C {
		public void start() {
			Assert.assertEquals(START_SEQ.size(), 1);
			Assert.assertEquals(START_SEQ.peekLast(), "A");
			START_SEQ.offerLast("C");
		}

		public void stop() {
			Assert.assertEquals(START_SEQ.pollLast(), "C");
		}
	}

	static class D {
		public D(A a, C c) {
			Assert.assertEquals(CTOR_SEQ.size(), 0);
			CTOR_SEQ.offerLast("D");
		}

		public void start() {
			Assert.assertEquals(START_SEQ.size(), 2);
			Assert.assertEquals(START_SEQ.peekLast(), "C");
			START_SEQ.offerLast("D");
		}

		public void stop() {
			Assert.assertEquals(START_SEQ.pollLast(), "D");
		}
	}

	static class E {
		public E(D d, B b) {
			Assert.assertEquals(CTOR_SEQ.size(), 2);
			Assert.assertEquals(CTOR_SEQ.peekLast(), "F");
			CTOR_SEQ.offerLast("E");
		}

		public void start() {
			Assert.assertEquals(START_SEQ.size(), 5);
			Assert.assertEquals(START_SEQ.peekLast(), "B");
			START_SEQ.offerLast("E");
		}

		public void stop() {
			Assert.assertEquals(START_SEQ.pollLast(), "E");
		}
	}

	static class F {
		public F(D d, C c) {
			Assert.assertEquals(CTOR_SEQ.size(), 1);
			Assert.assertEquals(CTOR_SEQ.peekLast(), "D");
			CTOR_SEQ.offerLast("F");
		}

		public void start() {
			Assert.assertEquals(START_SEQ.size(), 3);
			Assert.assertEquals(START_SEQ.peekLast(), "D");
			START_SEQ.offerLast("F");
		}

		public void stop() {
			Assert.assertEquals(START_SEQ.pollLast(), "F");
		}
	}

	static class G {
		public G(F f, E e) {
			Assert.assertEquals(CTOR_SEQ.size(), 3);
			Assert.assertEquals(CTOR_SEQ.peekLast(), "E");
			CTOR_SEQ.offerLast("G");
		}

		public void start() {
			Assert.assertEquals(START_SEQ.size(), 6);
			Assert.assertEquals(START_SEQ.peekLast(), "E");
			START_SEQ.offerLast("G");
		}

		public void stop() {
			Assert.assertEquals(START_SEQ.pollLast(), "G");
		}
	}

	@Test
	public void simpleDeps() throws Exception {
		CTOR_SEQ.clear();
		START_SEQ.clear();

		ActivationSet as = Crank.with(
			new A(), new B(), new C()
		).start(F.class, G.class);

		Assert.assertEquals(
			CTOR_SEQ.stream().collect(Collectors.joining()),
			"DFEG"
		);

		Assert.assertEquals(
			START_SEQ.stream().collect(Collectors.joining()),
			"ACDFBEG"
		);

		as.stop();
		Assert.assertTrue(START_SEQ.isEmpty());
	}

	static final ArrayDeque<String> CTOR_SEQ = new ArrayDeque<>();
	static final ArrayDeque<String> START_SEQ = new ArrayDeque<>();
}
