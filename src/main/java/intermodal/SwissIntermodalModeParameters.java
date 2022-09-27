package intermodal;

import org.eqasim.core.simulation.mode_choice.parameters.ModeParameters;


/**
 * @author kaghog created on 16.04.2021
 * @project swiss_intermodality
 */




public class SwissIntermodalModeParameters extends ModeParameters {
    public class SwissIntermodalWalkParameters {
        public double alpha_u = 0.0;
        public double betaTravelTime_u_min = 0.0;
    }

    public class SwissIntermodalBikeParameters {
        public double alpha_u = 0.0;
        public double BikeUsageTimePenalty_u_min = 0.0;
        public double betaTravelTime_u_min = 0.0;
    }

    public final SwissIntermodalWalkParameters aeWalk = new SwissIntermodalWalkParameters();
    public final SwissIntermodalBikeParameters aeBike = new SwissIntermodalBikeParameters();

    public static SwissIntermodalModeParameters buildDefault() {
        SwissIntermodalModeParameters parameters = new SwissIntermodalModeParameters();


        //AccessEgressWalk
        parameters.aeWalk.alpha_u = 0.0;
        parameters.aeWalk.betaTravelTime_u_min = -0.0;

        //AccessEgressDrt

        parameters.aeBike.alpha_u = 0.0;
        parameters.aeBike.BikeUsageTimePenalty_u_min = 2*1.7; //ToDo confirm this is correct - inconvenience of fetching the bike in the garage (2 x 1.7 minutes per leg)
        parameters.aeBike.betaTravelTime_u_min = -0.0;

        return parameters;
    }
}
