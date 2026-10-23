package com.koletar.jj.mineresetlite;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Command {

    String[] aliases();

    String usage() default "";

    String description();

    String[] help() default {};

    String[] permissions() default {};

    int min() default 0;

    int max() default -1;

    boolean onlyPlayers() default false;
}
