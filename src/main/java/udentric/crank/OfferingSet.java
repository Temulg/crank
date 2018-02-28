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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;

class OfferingSet {
	void addClassOffering(ClassOffering off) {
		if (classOfferings.containsKey(off.offeredClass()))
			throw new IllegalStateException(String.format(
				"Class %s is already on offer", off
			));

		classOfferings.put(off.offeredClass(), off);
	}

	void addClassOffering(AbstractClassOffering off) {
		abstractClassOfferings.put(off.offeredClass(), off);
	}

	boolean satisfy(Requirement req) {
		if (req instanceof ClassRequirement)
			return satisfyClassReq((ClassRequirement)req);

		return false;
	}

	private boolean satisfyClassReq(ClassRequirement req) {
		{
			ClassOffering off = classOfferings.get(
				req.requiredClass()
			);
			if (off != null) {
				req.fullfill(off);
				return true;
			}
		}

		Set<AbstractClassOffering> s = abstractClassOfferings.get(
			req.requiredClass()
		);
		Iterator<AbstractClassOffering> iter = s.iterator();
		if (!iter.hasNext())
			return false;

		AbstractClassOffering off = iter.next();
		int badness = off.badness();

		while (iter.hasNext()) {
			AbstractClassOffering nextOff = iter.next();
			if (nextOff.badness() < badness)
				off = nextOff;
		}

		req.fullfill(off);
		return true;
	}

	private final HashMap<
		Class<?>, ClassOffering
	> classOfferings = new HashMap<>();
	private final SetMultimap<
		Class<?>, AbstractClassOffering
	> abstractClassOfferings = MultimapBuilder.SetMultimapBuilder
		.hashKeys().hashSetValues().build();
}
