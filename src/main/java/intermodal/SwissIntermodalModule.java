package intermodal;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.eqasim.core.components.config.EqasimConfigGroup;
import org.eqasim.core.simulation.mode_choice.AbstractEqasimExtension;
import org.eqasim.core.simulation.mode_choice.ParameterDefinition;
import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;
import org.matsim.core.config.CommandLine;

import java.io.File;

public class SwissIntermodalModule extends AbstractEqasimExtension {

    private final CommandLine commandLine;

    public SwissIntermodalModule(CommandLine commandLine) {
        this.commandLine = commandLine;
    }

    @Override
    protected void installEqasimExtension() {
        bind(ModeParameters.class).to(SwissIntermodalModeParameters.class).asEagerSingleton();
    }

    @Provides
    @Singleton
    public SwissIntermodalModeParameters provideSwissIntermodalModeParameters(EqasimConfigGroup config) {
        SwissIntermodalModeParameters parameters = SwissIntermodalModeParameters.buildDefault();

        if (config.getModeParametersPath() != null) {
            ParameterDefinition.applyFile(new File(config.getModeParametersPath()), parameters);
        }

        ParameterDefinition.applyCommandLine("mode-parameter", commandLine, parameters);
        return parameters;
    }
}
