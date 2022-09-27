package simulation.utils;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.CommandLine;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.router.TripStructureUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.List;

public class CreateTripBasedPopulation {
    public static void main(String[] args)throws IOException, CommandLine.ConfigurationException {

        CommandLine cmd = (new CommandLine.Builder(args))
                .requireOptions("input-path", "output-path")
                .build();

        String population = cmd.getOptionStrict("input-path");
        String outputfile = cmd.getOptionStrict("output-path");

        /*String population = "/home/kaghog/projects/scenarios/zurich/zurich_population_1pct.xml.gz";
        String outputfile = "/home/kaghog/projects/scenarios/zurich/zurich_population_1pct_ptTripsgreater1km.xml.gz";*/

        BufferedWriter writer = IOUtils.getBufferedWriter("/home/kaghog/projects/scenarios/zurich/zurich_population_1pct_ptTripsgreater1km.txt");


        Config config = ConfigUtils.createConfig();
        Scenario scenario = ScenarioUtils.createScenario(config);
        PopulationReader popReader = new PopulationReader(scenario);
        popReader.readFile(population);

        ScenarioUtils.loadScenario(scenario);

        Population populationData = scenario.getPopulation();
        PopulationFactory populationFactory = populationData.getFactory();

        Population newPop = PopulationUtils.createPopulation(scenario.getConfig());

        //filter out trips less than 1km
        int m =1;
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

                    m++;
                }
                n++; //placed here instead of under the filtering allows one to know the trip stage

            }
        }
        System.out.println(m);
        writer.flush();
        writer.close();

        new PopulationWriter(newPop).write(outputfile);

        System.out.println("Finished");


    }
}
