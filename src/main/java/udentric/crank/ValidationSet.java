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
import java.util.Iterator;

class ValidationSet {
	ValidationSet(Iterator<Unit> iter) {
		while (iter.hasNext()) {
			Unit u = iter.next();
			if (u.value() != null) {
				u.collectOfferings(offSet);
				entries.add(new Entry(u, 0));
				continue;
			}

			int variant = u.selectConstructor(offSet);
			if (variant < 0)
				throw new IllegalStateException(String.format(
					"Not enough prerequisites to construct object of %s",
					u.cls
				));

			u.collectOfferings(offSet);
			entries.add(new Entry(u, variant));
		}
	}

	static class Entry {
		Entry(Unit unit_, int variant_) {
			unit = unit_;
			variant = variant_;
		}

		final Unit unit;
		final int variant;
	}

	final OfferingSet offSet = new OfferingSet();
	final ArrayList<Entry> entries = new ArrayList<>();
}
