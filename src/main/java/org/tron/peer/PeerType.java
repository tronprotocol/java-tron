/*
 * java-tron is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * java-tron is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.tron.peer;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

import static org.tron.peer.Peer.PEER_NORMAL;
import static org.tron.peer.Peer.PEER_SERVER;

public class PeerType implements IParameterValidator {
    @Override
    public void validate(String name, String value) throws ParameterException {
        if (!(value.equals(PEER_NORMAL) || value.equals(PEER_SERVER))) {
            throw new ParameterException("parameter " + name + " should be '" + PEER_NORMAL + "' or '" + PEER_SERVER
                    + "' (found " + value + ")");
        }
    }
}
