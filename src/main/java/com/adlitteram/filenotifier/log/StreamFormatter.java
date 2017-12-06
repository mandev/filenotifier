/* 
 * Copyright 2017 Emmanuel Deviller.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.adlitteram.filenotifier.log;

import java.util.logging.SimpleFormatter;
import java.util.logging.LogRecord;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.apache.commons.lang3.time.FastDateFormat;

public class StreamFormatter extends SimpleFormatter {

    protected boolean isBrief = false;
    protected boolean isTrunc = true;
    protected boolean withLevel = true;
    protected boolean withMethods = true;
    protected boolean withClasses = false;
    protected String separator = " # ";

    protected static final FastDateFormat FMT = FastDateFormat.getInstance("MM/dd/yyyy HH:mm:ss.SSS");
    protected static final String EOL = System.getProperty("line.separator");

    public void setSeparator(String separator) {
        this.separator = separator;
    }

    public void setBrief(boolean isBrief) {
        this.isBrief = isBrief;
    }

    public void setTruncateLoggerName(boolean isTrunc) {
        this.isTrunc = isTrunc;
    }

    public void setWithLevel(boolean how) {
        withLevel = how;
    }

    public void setWithMethods(boolean how) {
        withMethods = how;
    }

    public void setWithClasses(boolean how) {
        withClasses = how;
    }

    public StreamFormatter() {
    }

    public StreamFormatter(boolean isBrief) {
        this(isBrief, false);
    }

    public StreamFormatter(boolean isBrief, boolean isTrunc) {
        this.isBrief = isBrief;
        this.isTrunc = isTrunc;
    }

    @Override
    public String format(LogRecord rec) {
        StringBuilder b = new StringBuilder();
        b.append(FMT.format(rec.getMillis()));
        if (!isBrief) {
            b.append(" [");
            if (isTrunc) {
                String loggerName = rec.getLoggerName();
                if (loggerName != null) {
                    String[] s = rec.getLoggerName().split("\\.");
                    for (int i = 0; i < s.length - 1; ++i) {
                        b.append(s[i].charAt(0));
                        b.append(".");
                    }
                    b.append(s[s.length - 1]);
                }
                else {
                    b.append("Anonymous");
                }
            }
            else {
                b.append(rec.getLoggerName());
                b.append(separator);
                b.append(rec.getSequenceNumber());
            }
            b.append("]");
        }

        if (withLevel) {
            b.append(separator);
            b.append(rec.getLevel());
            b.append(separator);
        }

        if (withClasses) {
            b.append(": from=");
            b.append(rec.getSourceClassName());
            b.append(".");
        }

        if (withMethods) {
            b.append(rec.getSourceMethodName());
            b.append("(");
            Object a[] = rec.getParameters();
            if (a != null) {
                b.append(" ");
                for (int i = 0; i < a.length; ++i) {
                    if (i > 0) {
                        b.append(", ");
                    }
                    b.append(a[i]).append("");
                }
            }
            b.append(" ) ");
        }

        b.append(formatMessage(rec));

        if (rec.getThrown() != null) {
            StringWriter wr = new StringWriter();
            rec.getThrown().printStackTrace(new PrintWriter(wr));
            b.append(EOL);
            b.append(wr.toString());
        }
        b.append(EOL);
        return b.toString();
    }
}
