package simulation.utils;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.utils.geometry.CoordUtils;

/**
 * @author kaghog created on 13.09.2022
 * @project intermodality
 */
public class AdjustPopulation {
    public AdjustPopulation() {
    }

    public static void setIntermodalPersonFilterAttribute(Scenario scenario, String attributeType){
        String filterAttribute = "bikeIntermodalAvailable";
        String filterValue = "true";
        for (final Person person : scenario.getPopulation().getPersons().values()) {

        	Boolean hasbike = !(person.getAttributes().getAttribute("bikeAvailability").toString().equals("FOR_NONE"));
            Boolean hasPtSubscription = (boolean) person.getAttributes().getAttribute("ptHasGA") ||
                    (boolean) person.getAttributes().getAttribute("ptHasHalbtax") ||
                    (boolean) person.getAttributes().getAttribute("ptHasStrecke") ||
                    (boolean) person.getAttributes().getAttribute("ptHasVerbund");
            if (attributeType.equals("bikeOwnership") && hasbike){
                person.getAttributes().putAttribute(filterAttribute, filterValue);
            }
            else if (attributeType.equals("bikeOwnershipPTSubscription") && hasbike && hasPtSubscription){
                person.getAttributes().putAttribute(filterAttribute, filterValue);
            }else {
                person.getAttributes().putAttribute(filterAttribute, "false");
            }
        }
    }

    public static void CreateTripBasedPopulation(Scenario scenario){
        Population populationData = scenario.getPopulation();
        PopulationFactory populationFactory = populationData.getFactory();

        Population newPop = PopulationUtils.createPopulation(scenario.getConfig());

        //filter out trips less than 1km
        for (final Person person : scenario.getPopulation().getPersons().values()) {
            List<TripStructureUtils.Trip> trips = TripStructureUtils.getTrips(person.getSelectedPlan().getPlanElements());
            int n = 11;
            for (TripStructureUtils.Trip trip : trips) {
                double distance = CoordUtils.calcEuclideanDistance(trip.getOriginActivity().getCoord(), trip.getDestinationActivity().getCoord());
                if (distance >= 1000) {
                    Person newPerson = populationFactory.createPerson(Id.create(person.getId().toString() + n, Person.class));
                    Plan p = populationFactory.createPlan();


                    //add first activity to plan
                    p.addActivity(trip.getOriginActivity());

                    //remove the route and set the mode
                    for (Leg leg : trip.getLegsOnly()) {
                        if (leg.getRoute() != null) {
                            leg.setRoute(null);
                        }
                        leg.setMode("pt");
                        leg.getAttributes().putAttribute("routingMode", leg.getMode());

                        p.addLeg(leg);
                    }


                    //add second activity
                    p.addActivity(trip.getDestinationActivity());

                    //add plan to person
                    newPerson.addPlan(p);

                    //add the person attributes
                    newPerson.getAttributes().putAttribute("age", person.getAttributes().getAttribute("age"));
                    newPerson.getAttributes().putAttribute("bikeAvailability", person.getAttributes().getAttribute("bikeAvailability"));
                    newPerson.getAttributes().putAttribute("carAvail", person.getAttributes().getAttribute("carAvail"));
                    newPerson.getAttributes().putAttribute("employed", person.getAttributes().getAttribute("employed"));
                    newPerson.getAttributes().putAttribute("hasLicense", person.getAttributes().getAttribute("hasLicense"));
                    newPerson.getAttributes().putAttribute("home_x", person.getAttributes().getAttribute("home_x"));
                    newPerson.getAttributes().putAttribute("home_y", person.getAttributes().getAttribute("home_y"));
                    newPerson.getAttributes().putAttribute("isCarPassenger", person.getAttributes().getAttribute("isCarPassenger"));
                    newPerson.getAttributes().putAttribute("isOutside", person.getAttributes().getAttribute("isOutside"));
                    newPerson.getAttributes().putAttribute("mzHeadId", person.getAttributes().getAttribute("mzHeadId"));
                    newPerson.getAttributes().putAttribute("mzPersonId", person.getAttributes().getAttribute("mzPersonId"));
                    newPerson.getAttributes().putAttribute("ptHasGA", person.getAttributes().getAttribute("ptHasGA"));
                    newPerson.getAttributes().putAttribute("ptHasHalbtax", person.getAttributes().getAttribute("ptHasHalbtax"));
                    newPerson.getAttributes().putAttribute("ptHasStrecke", person.getAttributes().getAttribute("ptHasStrecke"));
                    newPerson.getAttributes().putAttribute("ptHasVerbund", person.getAttributes().getAttribute("ptHasVerbund"));
                    newPerson.getAttributes().putAttribute("spRegion", person.getAttributes().getAttribute("spRegion"));
                    newPerson.getAttributes().putAttribute("statpopHouseholdId", person.getAttributes().getAttribute("statpopHouseholdId"));
                    newPerson.getAttributes().putAttribute("statpopPersonId", person.getAttributes().getAttribute("statpopPersonId"));

                    //add original personID
                    newPerson.getAttributes().putAttribute("origPersonId", person.getId().toString());

                    newPop.addPerson(newPerson);
                }

            }

        }
    }
}
