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
package org.tron.datasource;


public abstract class AbstractChainedSource<Key, Value, SourceKey, SourceValue> implements Source<Key, Value> {

    private Source<SourceKey, SourceValue> source;
    protected boolean flushSource;

    public AbstractChainedSource(Source<SourceKey, SourceValue> source) {
        this.source = source;
    }

    public Source<SourceKey, SourceValue> getSource() {
        return source;
    }

    public synchronized boolean flush() {
        boolean ret = flushImpl();
        if (flushSource) {
            ret |= getSource().flush();
        }
        return ret;
    }

    /**
     * Should be overridden to do actual source flush
     */
    protected abstract boolean flushImpl();

    public void setFlushSource(boolean flushSource) {
        this.flushSource = flushSource;
    }


}
