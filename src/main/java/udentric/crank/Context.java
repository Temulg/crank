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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedAcyclicGraph;
import org.jgrapht.io.DOTExporter;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

public class Context implements AutoCloseable {
	Context() {
	}

	@Override
	public void close() {
		stop();
	}

	public Context start(Class<?>... clss) throws Exception {
		LinkedList<Requirement> rl = new LinkedList<>();

		for (Class<?> cls: clss) {
			Unit u = new Unit(cls);
			u.collectOfferings(offSet);
			u.collectRequirements(rl);
			targets.add(u);
		}

		boolean newReqs = resolveOnce(rl);
		while (newReqs)
			newReqs = resolveOnce(rl);

		targets.forEach(this::updateDepGraph);

		DOTExporter<Unit, DefaultEdge> exp = new DOTExporter<>(
			u -> u.cls.getSimpleName() , null, null
		);

		exp.exportGraph(depGraph, System.out);

		return this;
	}

	public void stop() {

	}

	void appendExisting(Object[] objs) {
		for (Object obj: objs) {
			Unit u = new Unit(obj);
			u.collectOfferings(offSet);
		}
	}

	private boolean resolveOnce(List<Requirement> rl) {
		ListIterator<Requirement> iter = rl.listIterator();
		while (iter.hasNext()) {
			Requirement r = iter.next();
			if (offSet.satisfy(r)) {
				iter.remove();
			}
		}

		ArrayList<Requirement> nl = new ArrayList<>();

		iter = rl.listIterator();
		while (iter.hasNext()) {
			Requirement r = iter.next();
			Unit u = r.makeUnit();
			if (u == null)
				continue;

			u.collectOfferings(offSet);
			u.collectRequirements(nl);
		}

		if (!nl.isEmpty()) {
			rl.addAll(nl);
			return true;
		} else
			return false;
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
					LOGGER.debug(
						"Dependency between {} and {} will introduce a cycle",
						u, tu
					);
				}
			}
		});
	}

	private final Logger LOGGER = LogManager.getLogger(Context.class);

	private final DirectedAcyclicGraph<
		Unit, DefaultEdge
	> depGraph = new DirectedAcyclicGraph<>(
		DefaultEdge.class
	);
	private final ArrayList<Unit> targets = new ArrayList<>();
	private final OfferingSet offSet = new OfferingSet();
}
