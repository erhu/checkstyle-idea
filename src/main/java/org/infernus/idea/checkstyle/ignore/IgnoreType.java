package org.infernus.idea.checkstyle.ignore;

/**
 * IgnoreType
 * Created by lisper on 15/11/17.
 */
public enum IgnoreType {
    FILE(0),
    DIR(1),
    FILE_TYPE(2); // *.class

    final int code;

    IgnoreType(int i) {
        this.code = i;
    }
}
