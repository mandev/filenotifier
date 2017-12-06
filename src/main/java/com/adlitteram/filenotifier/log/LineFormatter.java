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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.Date;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class LineFormatter extends Formatter {

    private final static String FORMAT = "{0,date} {0,time}";
    private final Date dat = new Date();
    private MessageFormat formatter;
    private final Object args[] = new Object[1];

    // Line separator string.  This is the value of the line.separator
    // property at the moment that the SimpleFormatter was created.
    private final String lineSeparator = "\n";

    /**
     * Format the given LogRecord.
     *
     * @param record the log record to be formatted.
     * @return a formatted log record
     */
    @Override
    public synchronized String format(LogRecord record) {
        StringBuilder sb = new StringBuilder();

        // Minimize memory allocations here.
        dat.setTime(record.getMillis());
        args[0] = dat;
        StringBuffer text = new StringBuffer();
        if (formatter == null) {
            formatter = new MessageFormat(FORMAT);
        }
        formatter.format(args, text, null);
        sb.append(text);
        sb.append(" ");

        if (record.getSourceClassName() != null) {
            String str = record.getSourceClassName();
            if (str.startsWith("com.adlitteram.edoc.")) {
                sb.append(str.substring(20));
            }
            else {
                sb.append(str);
            }
        }
        else {
            sb.append(record.getLoggerName());
        }

        if (record.getSourceMethodName() != null) {
            sb.append(" ");
            sb.append(record.getSourceMethodName());
        }

        sb.append(" ");
        sb.append(record.getLevel().getLocalizedName());
        sb.append(": ");
        sb.append(formatMessage(record));
        sb.append(lineSeparator);

        if (record.getThrown() != null) {
            try (StringWriter sw = new StringWriter();
                 PrintWriter pw = new PrintWriter(sw)) {
                record.getThrown().printStackTrace(pw);
                sb.append(sw.toString());
            }
            catch (Exception ex) {
                // TODO
            }

        }
        return sb.toString();
    }
}
