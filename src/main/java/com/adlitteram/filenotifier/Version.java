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
package com.adlitteram.filenotifier;

public class Version {

    private static final String COPYRIGHT = "Emmanuel Deviller";
    private static final String DATE = "2017";
    private static final String AUTHOR = "Emmanuel Deviller";
    private static final String BUILD_NUM = "240";
    private static final String LICENCE = "Commercial";
    private static final String VERSION_NUM = "1.03";
    private static final String CNAME = "FileNotifier";
    private static final String NAME = "FileNotifier";

    public static String getCNAME() {
        return CNAME;
    }

    public static String getNAME() {
        return NAME;
    }

    public static String getCOPYRIGHT() {
        return COPYRIGHT;
    }

    public static String getDATE() {
        return DATE;
    }

    public static String getAUTHOR() {
        return AUTHOR;
    }

    public static String getBUILD() {
        return BUILD_NUM;
    }

    public static String getLICENCE() {
        return LICENCE;
    }

    public static String getRELEASE() {
        return VERSION_NUM;
    }

    public static String getVERSION() {
        return VERSION_NUM;
    }
}
