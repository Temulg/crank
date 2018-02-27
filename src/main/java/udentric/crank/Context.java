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

import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class Context implements AutoCloseable {
	Context() {
	}

	@Override
	public void close() {
		stop();
	}

	public Context start(Class<?>... clss) {
		for (Class<?> cls: clss) {
			Unit u = new Unit(cls);
			u.addToClassMap(classToUnitMap);
			targets.add(u);
			depGraph.addVertex(u);
			allNeeded.addAll(u.neededTypes());
		}

		return this;
	}

	public void stop() {

	}

	void appendExisting(Object[] objs) {
		for (Object obj: objs) {
			Unit u = new Unit(obj);
			u.addToClassMap(classToUnitMap);
			depGraph.addVertex(u);
		}
	}

	private void addTargetEdges(Unit u) {
		u.neededTypes().forEach(cls -> {
			Set<Unit> deps = classToUnitMap.get(cls);
			if (deps.isEmpty()) {

			}

		});
	}

	private final SetMultimap<
		Class<?>, Unit
	> classToUnitMap = MultimapBuilder.SetMultimapBuilder
		.hashKeys().hashSetValues().build();
	private final DefaultDirectedWeightedGraph<
		Unit, DefaultWeightedEdge
	> depGraph = new DefaultDirectedWeightedGraph<>(
		DefaultWeightedEdge.class
	);
	private final ArrayList<Unit> targets = new ArrayList<>();
	private final HashSet<Class<?>> allNeeded = new HashSet<>();
}
