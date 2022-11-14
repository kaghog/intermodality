package ch.sbb.matsim.routing.pt.raptor;

import java.util.List;

import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.utils.misc.OptionalTime;

/**
 * @author kaghog created on 16.04.2021
 * @project swiss_intermodality
 */
import com.google.inject.Inject;

import intermodal.SwissIntermodalModeParameters;



public class SwissRaptorIntermodalAccessEgress implements RaptorIntermodalAccessEgress {

    final SwissIntermodalModeParameters modeParameters;

    @Inject
    public SwissRaptorIntermodalAccessEgress(SwissIntermodalModeParameters modeParameters) {

        this.modeParameters = modeParameters;

    }

    public RIntermodalAccessEgress calcIntermodalAccessEgress(
            final List<? extends PlanElement> legs, RaptorParameters params, Person person, RaptorStopFinder.Direction direction, double departureTime) {
        double disutility = 0.0;
        double tTime = 0.0;
        double bikeTimePenalty_u_s = modeParameters.aeBike.BikeUsageTimePenalty_u_min * 60;
        for (PlanElement pe : legs) {
            if (pe instanceof Leg) {
                String mode = ((Leg) pe).getMode();

                OptionalTime travelTime = ((Leg) pe).getTravelTime();

                if (travelTime.isDefined()) {
                    //penalty for bike travel, parking, lifting etc todo confirm this
                    if (mode.equals("bike")){
                    	tTime += travelTime.seconds() + bikeTimePenalty_u_s;
                        disutility += (travelTime.seconds() + bikeTimePenalty_u_s) * -params.getMarginalUtilityOfTravelTime_utl_s(mode);
                    } else {
                    	tTime += travelTime.seconds();
                        disutility += travelTime.seconds() * -params.getMarginalUtilityOfTravelTime_utl_s(mode); }
                }

            }
        }
        return new RIntermodalAccessEgress(legs, disutility, tTime, direction);
    }

    @Override
    public RIntermodalAccessEgress calcIntermodalAccessEgress(List<? extends PlanElement> legs, RaptorParameters params,
                                                              Person person, RaptorStopFinder.Direction direction) {
        // TODO Auto-generated method stub
        return null;
    }

}