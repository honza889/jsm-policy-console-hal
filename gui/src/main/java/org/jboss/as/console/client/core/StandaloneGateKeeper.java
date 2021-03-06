package org.jboss.as.console.client.core;

import com.gwtplatform.mvp.client.proxy.Gatekeeper;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * @author Heiko Braun
 * @date 1/26/12
 */
@Singleton
public class StandaloneGateKeeper implements Gatekeeper {


    private final BootstrapContext bootstrapContext;

    @Inject
    public StandaloneGateKeeper(final BootstrapContext bootstrapContext) {
        this.bootstrapContext = bootstrapContext;
    }

    @Override
    public boolean canReveal() {
        return bootstrapContext.isStandalone();
    }
}