package ru.mt.app;

import lombok.Getter;
import lombok.extern.log4j.Log4j2;

@Log4j2
public abstract class Component {
    @Getter
    private volatile boolean destroying = false;

    public void destroy() {
        log.debug("destroying " + this.getClass().getCanonicalName());
        destroying = true;
        destroyInternal();
    }

    protected void destroyInternal() {
        // if needed override at derived class
    }
}
