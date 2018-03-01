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

import java.lang.invoke.MethodHandle;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableTable;

class MethodSet {
	MethodSet(
		ImmutableList<MethodHandle> methods_,
		ImmutableTable<
			Integer, Integer, ClassRequirement
		> methodArgs_
	) {
		methods = methods_;
		methodArgs = methodArgs_;
	}

	void collectRequirements(List<Requirement> rl) {
		methodArgs.cellSet().forEach(c -> rl.add(c.getValue()));
	}

	int selectVariant(OfferingSet offSet) {
		int count = methods.size();

		nextCtor: for (int pos = 0; pos < count; pos++) {
			for (
				ClassRequirement cr:
				methodArgs.row(pos).values()
			) {
				if (!offSet.satisfy(cr))
					continue nextCtor;
			}
			return pos;
		}

		return -1;
	}

	Object make(int variant) throws Throwable {
		MethodHandle h = methods.get(variant);

		Object[] pReqs = methodArgs.row(
			variant
		).values().toArray();
		Object[] args = new Object[pReqs.length];
		for (int pos = 0; pos < pReqs.length; pos++) {
			ClassRequirement cr = (ClassRequirement)pReqs[pos];
			Unit ru = cr.getReferred();

			if (ru == null || ru.getValue() == null)
				return null;

			args[pos] = ru.getValue();
		}

		return h.invokeWithArguments(args);
	}

	final ImmutableList<MethodHandle> methods;
	final ImmutableTable<
		Integer, Integer, ClassRequirement
	> methodArgs;
}
