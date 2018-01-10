package org.tron.application;

import com.google.inject.Guice;
import com.google.inject.Injector;

public class ApplicationFactory {

    /**
     * Build a Guice instance
     *
     * @return Guice
     */
    public Injector buildGuice() {
        return Guice.createInjector(
            new Module());
    }

    /**
     * Build a new application
     *
     * @return
     */
    public Application build() {
        return new Application(buildGuice());
    }

    /**
     * Build a new cli application
     *
     * @return
     */
    public CliApplication buildCli() {
        return new CliApplication(buildGuice());
    }
}
