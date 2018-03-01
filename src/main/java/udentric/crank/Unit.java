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

import java.util.List;

interface Unit {
	void collectOfferings(OfferingSet offSet);

	void collectRequirements(List<Requirement> rl);

	int selectVariant(OfferingSet offSet);

	Object getValue();

	Object obtainValue(int variant) throws Throwable;

	static void collectOfferings(OfferingSet s, Class<?> cls, Unit self) {
		s.addClassOffering(new ClassOffering(self, cls));

		Class<?> sc = cls.getSuperclass();

		int badness = 1;

		while (sc != Object.class) {
			s.addClassOffering(
				new AbstractClassOffering(self, sc, badness)
			);
			badness++;
			sc = sc.getSuperclass();
		}

		collectInterfaces(s, cls.getInterfaces(), 1, self);
	}

	private static void collectInterfaces(
		OfferingSet s, Class<?>[] ifaces, int badness, Unit self
	) {
		for (Class<?> iface: ifaces) {
			s.addClassOffering(new AbstractClassOffering(
				self, iface, badness
			));
			collectInterfaces(
				s, iface.getInterfaces(), badness + 1, self
			);
		}
	}
}
