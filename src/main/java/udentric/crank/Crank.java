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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedAcyclicGraph;

public class Crank {
	private Crank() {
	}

	public static Crank with(Object... objs) {
		Crank cr = new Crank();
		cr.appendExisting(objs);
		return cr;
	}

	public Crank withSuppliers(Object... sups) throws Exception {
		for (Object sup: sups) {
			SupplierUnit.inspectSupplierMethods(sup).forEach(u -> {
				u.collectOfferings(offSet);
				u.collectRequirements(reqSet);
			});
		}
		return this;
	}

	public ActivationSet start(Class<?>... clss) throws Exception {
		for (Class<?> cls: clss) {
			Unit u = new ClassUnit(cls);
			u.collectOfferings(offSet);
			u.collectRequirements(reqSet);
			targets.add(u);
		}

		boolean newReqs = resolveOnce();
		while (newReqs)
			newReqs = resolveOnce();

		targets.forEach(this::updateDepGraph);

		ValidationSet vs = new ValidationSet(depGraph.iterator());

		ObjectActivator act = activator;

		if (act == null) {
			act = new StartStopActivator();
			act.setLogMessageFactory(this::newLogMessage);
		}

		return new ActivationSet(vs, act, this::newLogMessage);
	}

	public Crank withObjectActivator(ObjectActivator activator_) {
		activator = activator_;
		activator.setLogMessageFactory(this::newLogMessage);
		return this;
	}

	public Crank withLoggerContext(Map<String, String> logCtx_) {
		logCtx.putAll(logCtx_);
		return this;
	}

	private void appendExisting(Object[] objs) {
		for (Object obj: objs) {
			Unit u = new ObjectUnit(obj);
			u.collectOfferings(offSet);
		}
	}

	private boolean resolveOnce() throws Exception {
		ArrayList<Requirement> nl = new ArrayList<>();

		for (int pos = 0; pos < reqSet.size(); pos++) {
			Requirement r = reqSet.get(pos);

			if (r == null)
				continue;

			if (offSet.satisfy(r))
				reqSet.set(pos, null);
			else {
				Unit u = r.makeUnit();
				if (u != null) {
					u.collectOfferings(offSet);
					u.collectRequirements(nl);
				}
			}
		}

		if (nl.isEmpty())
			return false;

		int nlPos = 0;
		for (int pos = 0; pos < reqSet.size(); pos++) {
			if (reqSet.get(pos) != null)
				continue;

			reqSet.set(pos, nl.get(nlPos));
			nlPos++;
			if (nlPos == nl.size())
				return true;
		}

		for (; nlPos < nl.size(); nlPos++)
			reqSet.add(nl.get(nlPos));

		return true;
	}

	private void updateDepGraph(Unit u) {
		depGraph.addVertex(u);
		ArrayList<Requirement> rl = new ArrayList<>();
		u.collectRequirements(rl);
		rl.forEach(r -> {
			Unit tu = r.getReferred();
			if (tu != null) {
				updateDepGraph(tu);
				try {
					depGraph.addEdge(tu, u);
				} catch (IllegalArgumentException e) {
					LOGGER.debug(() -> newLogMessage(
						"cyclical dependency detected"
					).with(
						"src_unit", tu
					).with(
						"dst_unit", u
					));
				}
			}
		});
	}

	public LogMessage newLogMessage(String msg) {
		return new LogMessage(msg, logCtx);
	}

	static private final Logger LOGGER = LogManager.getLogger();

	private final DirectedAcyclicGraph<
		Unit, DefaultEdge
	> depGraph = new DirectedAcyclicGraph<>(
		DefaultEdge.class
	);
	private final ArrayList<Unit> targets = new ArrayList<>();
	private final OfferingSet offSet = new OfferingSet();
	private final ArrayList<Requirement> reqSet = new ArrayList<>();
	private final LinkedHashMap<
		String, String
	> logCtx = new LinkedHashMap<>();
	private ObjectActivator activator;
}
