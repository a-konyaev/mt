package ru.mt.app;

import lombok.extern.log4j.Log4j2;

@Log4j2
public abstract class Component {
    public void destroy() {
       log.debug("destroying " + this.getClass().getCanonicalName());

        // if needed override in derived class
    }
}
